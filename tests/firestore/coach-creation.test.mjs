import assert from 'node:assert/strict';
import {test,before,after,beforeEach} from 'node:test';
import {readFileSync} from 'node:fs';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc,updateDoc,getDocFromServer,deleteDoc,writeBatch,runTransaction,serverTimestamp,deleteField} from 'firebase/firestore';
let env;
const db=uid=>env.authenticatedContext(uid,{email:uid+'@example.test'}).firestore();
const profile=(uid,role,extra={})=>({uid,role,active:true,name:uid,children:{},...extra});
const id=(uid,nonce='AbCdEfGhIjKlMnOpQr12')=>'coach_'+uid+'_'+nonce;
const team=(uid,tid)=>({id:tid,name:'Nuevo equipo',category:'SUB_15',description:'',ownerId:uid,createdAt:1,updatedAt:1,active:true,coachCreated:true});
const get=(d,path)=>getDocFromServer(doc(d,path));
async function seed(rows){await env.withSecurityRulesDisabled(async c=>{for(const [p,v]of Object.entries(rows))await setDoc(doc(c.firestore(),p),v)})}
const fields=(uid,tid,old=['a'])=>({coachTeamIds:[...old,tid],lastCreatedTeamId:tid,coachScopeBy:uid,coachScopeAt:serverTimestamp()});
async function create(uid='coach',tid=id(uid),options={}){
 const d=db(uid),b=writeBatch(d);
 if(!options.skipTeam)b.set(doc(d,'teams',tid),{...team(uid,tid),...options.team});
 if(!options.skipUser)b.update(doc(d,'users',uid),{...fields(uid,tid,options.old??['a']),...options.fields});
 return b.commit();
}
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-sportpro-security',firestore:{host:'127.0.0.1',port:8193,rules:readFileSync(process.env.SPORTPRO_RULES_FILE||new URL('../../firestore.rules',import.meta.url),'utf8')}})});
after(async()=>{await env.cleanup()});
beforeEach(async()=>{await env.clearFirestore();await seed({
 'users/admin':profile('admin','ADMIN'),'users/coach':profile('coach','ENTRENADOR',{coachTeamIds:['a']}),
 'users/empty':profile('empty','ENTRENADOR',{coachTeamIds:[]}), 'users/legacy':profile('legacy','ENTRENADOR',{teamId:'a'}),
 'users/parent':profile('parent','PADRE',{coachTeamIds:[]}), 'users/player':profile('player','JUGADOR',{coachTeamIds:[]}),
 'users/off':profile('off','ENTRENADOR',{coachTeamIds:[],active:false}),
 'teams/a':{name:'Propio',ownerId:'legacy',active:true},'teams/b':{name:'Ajeno',ownerId:'admin',active:true},
 'teams/b/players/minor':{name:'Privado',active:true}
 })});
test('T1 own team + scope commits atomically; old scope stays, foreign team stays denied',async()=>{
 const tid=id('coach');await assertSucceeds(create());
 assert.deepEqual((await get(db('coach'),'users/coach')).data().coachTeamIds,['a',tid]);
 await assertSucceeds(get(db('coach'),'teams/'+tid));await assertSucceeds(get(db('coach'),'teams/a'));
 await assertFails(get(db('coach'),'teams/b/players/minor'));await assertFails(get(db('parent'),'teams/'+tid));
});
test('T2 approved coach with explicit empty scope may create its first team',async()=>{await assertSucceeds(create('empty',id('empty'),{old:[]}));});
test('T3 legacy scope requires explicit ADMIN migration; existing grant is preserved',async()=>{
 await assertFails(create('legacy',id('legacy'),{old:[]}));
 await assertSucceeds(updateDoc(doc(db('admin'),'users/legacy'),{coachTeamIds:['a'],coachScopeBy:'admin',coachScopeAt:serverTimestamp()}));
 await assertSucceeds(create('legacy'));await assertSucceeds(get(db('legacy'),'teams/a'));
});
test('T4 neither missing half can commit',async()=>{
 await assertFails(create('coach',id('coach'),{skipTeam:true}));await assertFails(create('coach',id('coach'),{skipUser:true}));
 assert.deepEqual((await get(db('coach'),'users/coach')).data().coachTeamIds,['a']);
});
test('T5 existing foreign or owned teams cannot grant extra scope even with overwrite',async()=>{
 await assertFails(create('coach','b'));await assertFails(create('coach','a'));
 const tid=id('coach');await seed({['teams/'+tid]:team('admin',tid),['teams/'+tid+'/players/minor']:{name:'Private'}});
 await assertFails(create('coach',tid));await assertFails(get(db('coach'),'teams/'+tid+'/players/minor'));
});
test('T6 new foreign namespace and legacy auto-ID cannot be claimed',async()=>{
 await assertFails(create('coach',id('other')));await assertFails(create('coach','AbCdEfGhIjKlMnOpQr12'));
});
test('T7 self-creation cannot change role, children, active state or another account',async()=>{
 for(const extra of [{role:'ADMIN'},{children:{b:{minor:{code:'FAKE'}}}},{name:'Escalation'},{active:false}])await assertFails(create('coach',id('coach'),{fields:extra}));
 const d=db('coach'),b=writeBatch(d);b.set(doc(d,'teams',id('empty')),team('empty',id('empty')));b.update(doc(d,'users/empty'),fields('empty',id('empty'),[]));await assertFails(b.commit());
});
test('T8 actor and timestamp must be exact server audit, not omitted or forged',async()=>{
 for(const extra of [{coachScopeBy:'admin'},{coachScopeAt:1},{lastCreatedTeamId:'b'},{coachScopeAt:deleteField()}])await assertFails(create('coach',id('coach'),{fields:extra}));
});
test('T9 inactive coach, parent, player and unauthenticated caller cannot create',async()=>{
 for(const uid of ['off','parent','player'])await assertFails(create(uid,id(uid),{old:[]}));
 await assertFails(setDoc(doc(env.unauthenticatedContext().firestore(),'teams',id('coach')),team('coach',id('coach'))));
});
test('T10 cap 20 and append-only list cannot drop reorder or smuggle foreign grants',async()=>{
 for(const list of [[],['b',id('coach')],[id('coach'),'a'],['a',id('coach'),'b']])await assertFails(create('coach',id('coach'),{fields:{coachTeamIds:list}}));
 const old=Array.from({length:19},(_,i)=>'existing'+i);await seed({'users/coach':profile('coach','ENTRENADOR',{coachTeamIds:old})});
 await assertSucceeds(create('coach',id('coach'),{old}));await assertFails(create('coach',id('coach','BcDeFgHiJkLmNoPqRs23'),{old:[...old,id('coach')]}));
});
test('T11 two team roots cannot piggyback on a single scope audit',async()=>{
 const d=db('coach'),tid=id('coach'),other=id('coach','BcDeFgHiJkLmNoPqRs23'),b=writeBatch(d);
 b.set(doc(d,'teams',tid),team('coach',tid));b.set(doc(d,'teams',other),team('coach',other));b.update(doc(d,'users/coach'),fields('coach',tid));await assertFails(b.commit());
});
test('T12 concurrent creates at scope size 19 yield one winner, 20 scopes and no lost update',async()=>{
 const old=Array.from({length:19},(_,i)=>'existing'+i);await seed({'users/coach':profile('coach','ENTRENADOR',{coachTeamIds:old})});
 const createTx=nonce=>{const d=db('coach'),tid=id('coach',nonce);return runTransaction(d,async tx=>{
  const ref=doc(d,'users/coach'),row=(await tx.get(ref)).data();tx.set(doc(d,'teams',tid),team('coach',tid));tx.update(ref,fields('coach',tid,row.coachTeamIds));
 })};
 const results=await Promise.allSettled([createTx('AbCdEfGhIjKlMnOpQr12'),createTx('BcDeFgHiJkLmNoPqRs23')]);
 assert.equal(results.filter(x=>x.status==='fulfilled').length,1);assert.equal((await get(db('coach'),'users/coach')).data().coachTeamIds.length,20);
});
test('T13 creation validates owner, seal, id, category, active and bounded payload',async()=>{
 for(const changed of [{ownerId:'admin'},{id:'b'},{coachCreated:false},{category:'FAKE'},{createdAt:'not-an-integer'},{active:false},{name:''},{name:'x'.repeat(121)},{extra:'field'}])await assertFails(create('coach',id('coach'),{team:changed}));
 for(const tid of [id('coach','short'),id('coach','AbCdEfGhIjKlMnOpQr!2')])await assertFails(create('coach',tid));
});
test('T14 created roots cannot be deleted or unsealed, but normal archival works',async()=>{
 const tid=id('coach');await assertSucceeds(create());
 for(const actor of ['coach','admin']){
  await assertFails(deleteDoc(doc(db(actor),'teams',tid)));
  await assertFails(updateDoc(doc(db(actor),'teams',tid),{coachCreated:deleteField()}));
 }
 await assertSucceeds(updateDoc(doc(db('coach'),'teams',tid),{active:false}));
 await assertSucceeds(updateDoc(doc(db('admin'),'teams',tid),{active:true}));
});
test('T15 ADMIN ordinary create remains valid and does not need a self-scope grant',async()=>{
 await assertSucceeds(setDoc(doc(db('admin'),'teams/admin-new'),{name:'Admin',ownerId:'admin',active:true}));
 await assertFails(get(db('coach'),'teams/admin-new'));
});
test('T16 UID regex metacharacters are literal, not namespace wildcards',async()=>{
 const uid='coach[.*]';await seed({['users/'+uid]:profile(uid,'ENTRENADOR',{coachTeamIds:[]})});
 await assertSucceeds(create(uid,id(uid),{old:[]}));
 await assertFails(create(uid,id('coach-anything'),{old:[id(uid)]}));
});

test('T17 neither coach nor ADMIN may add or change a creation seal on legacy roots',async()=>{
 for(const actor of ['coach','admin']) for(const value of [true,false,null])
  await assertFails(updateDoc(doc(db(actor),'teams/a'),{coachCreated:value}));
 await assertSucceeds(updateDoc(doc(db('admin'),'teams/a'),{name:'Edición legítima'}));
 await assertSucceeds(deleteDoc(doc(db('admin'),'teams/a')));
});
