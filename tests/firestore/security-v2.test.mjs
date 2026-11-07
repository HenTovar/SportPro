import assert from 'node:assert/strict';
import {test,before,after,beforeEach} from 'node:test';
import {readFileSync} from 'node:fs';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,collection,getDoc,getDocFromServer,getDocs,setDoc,updateDoc,deleteDoc,query,where,limit,runTransaction,writeBatch,serverTimestamp,Timestamp,deleteField,onSnapshot,disableNetwork,enableNetwork} from 'firebase/firestore';
let env;const db=uid=>env.authenticatedContext(uid,{email:uid+'@example.test',email_verified:true}).firestore();
const profile=(uid,role,extra={})=>({uid,role,name:uid,email:uid+'@example.test',active:true,teamId:'',playerId:'',...extra});
const stamp=()=>serverTimestamp();const ago=ms=>Timestamp.fromMillis(Date.now()-ms);
const put=(d,path,data)=>setDoc(doc(d,path),data);const get=(d,path)=>getDocFromServer(doc(d,path));
async function seed(rows){await env.withSecurityRulesDisabled(async c=>{for(const[p,v]of Object.entries(rows))await put(c.firestore(),p,v)})}
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-sportpro-security',firestore:{host:'127.0.0.1',port:8193,rules:readFileSync(process.env.SPORTPRO_RULES_FILE||new URL('../../firestore.rules',import.meta.url),'utf8')}})});
after(async()=>{if(env)await env.cleanup()});
beforeEach(async()=>{await env.clearFirestore();await seed({
 'users/admin':profile('admin','ADMIN'),'users/coachA':profile('coachA','ENTRENADOR',{coachTeamIds:['a']}),
 'users/coachB':profile('coachB','ENTRENADOR',{coachTeamIds:['b']}),
 'users/legacy':profile('legacy','ENTRENADOR',{teamId:'a'}),'users/owner':profile('owner','ENTRENADOR'),
 'users/empty':profile('empty','ENTRENADOR',{coachTeamIds:[]}),
 'users/inactive':profile('inactive','ENTRENADOR',{coachTeamIds:['a'],active:false,isActive:true}),
 'users/playerA':profile('playerA','JUGADOR',{teamId:'a',playerId:'one'}),
 'users/playerB':profile('playerB','JUGADOR',{teamId:'b',playerId:'one'}),
 'users/parent':profile('parent','PADRE'), 'users/other':profile('other','PADRE'),
 'users/guest':profile('guest','JUGADOR'),
 'teams/a':{name:'A',ownerId:'owner',active:true},'teams/b':{name:'B',ownerId:'admin',active:true},
 'teams/a/players/one':{name:'Menor A',active:true,parentUid:null},'teams/a/players/two':{name:'Hermano A',active:true,parentUid:null},
 'teams/b/players/one':{name:'Menor B',active:true,parentUid:null},
 'teams/a/trainings/t':{name:'Sesión A',active:true},'teams/b/trainings/t':{active:true},
 'teams/a/attendance/x':{playerId:'one',trainingId:'t'},'teams/a/attendance/y':{playerId:'two',trainingId:'t'},
 'teams/b/attendance/x':{playerId:'one',trainingId:'t'},
 'matches/a':{teamA:'a',teamB:'outside',eventVersion:0,status:'not_started'},
 'matches/b':{teamA:'b',teamB:'outside',eventVersion:0,status:'not_started'},
 'matches/shared':{teamA:'a',teamB:'b',eventVersion:0,status:'not_started'},
 'matches/a/eventTypes/goal':{label:'Gol',countsGoal:true,active:true},
 'matches/shared/eventTypes/goal':{label:'Gol',countsGoal:true,active:true},
 'teams/a/posts/p':{text:'Muro A',state:'VISIBLE',authorUid:'playerA'},
 'teams/b/posts/p':{text:'Muro B',state:'VISIBLE',authorUid:'playerB'}
})});
const codeAt=n=>'ABCDEFGH'+String(n).padStart(4,'2').replaceAll('0','2').replaceAll('1','3');
async function issue(uid,code='ABCDEFGH2345',team='a',player='one'){
 const d=db(uid),ref=doc(d,'familyCodes',code),quota=doc(d,'familyCodeQuotas',uid);
 await runTransaction(d,async tx=>{const q=await tx.get(quota);assert.equal((await tx.get(ref)).exists(),false);
 const old=q.data();const restart=!old||old.windowStart.toMillis()+3600000<=Date.now();
 tx.set(ref,{teamId:team,playerId:player,issuedBy:uid,createdAt:stamp(),expiresAt:Timestamp.fromMillis(Date.now()+1200000),usedBy:'',usedAt:null});
 tx.set(quota,{windowStart:restart?stamp():old.windowStart,count:restart?1:old.count+1,lastCode:code});});return code;
}
async function redeem(uid,code='ABCDEFGH2345',extra={}){
 const d=db(uid);await runTransaction(d,async tx=>{const account=(await tx.get(doc(d,'users',uid))).data(),gift=(await tx.get(doc(d,'familyCodes',code))).data();
 const children={...(account.children||{})};children[gift.teamId]={...(children[gift.teamId]||{}),[gift.playerId]:{code}};
 tx.update(doc(d,'users',uid),{children,lastFamilyCode:code,...extra});
 tx.update(doc(d,'familyCodes',code),{usedBy:uid,usedAt:stamp(),name:deleteField()});
 tx.update(doc(d,'teams',gift.teamId,'players',gift.playerId),{parentUid:uid});});
}
async function revoke(uid='parent',team='a',player='one',actor='admin',options={}){
 const d=db(actor);return runTransaction(d,async tx=>{const old=(await tx.get(doc(d,'users',uid))).data(),child=(await tx.get(doc(d,'teams',team,'players',player))).data();
 const children=structuredClone(old.children||{});if(children[team]){delete children[team][player];if(!Object.keys(children[team]).length)delete children[team];}
 const fields={children,lastFamilyRevocation:{teamId:team,playerId:player,by:actor,at:stamp()},...options.extra};
 if(old.teamId===team&&old.playerId===player){fields.teamId='';fields.playerId='';}
 if(!options.skipProfile)tx.update(doc(d,'users',uid),fields);
 if(!options.skipChild)tx.update(doc(d,'teams',team,'players',player),{parentUid:child.parentUid===uid?null:child.parentUid,familyRevokedAt:stamp(),familyRevokedParentUid:uid,...options.child});});
}
async function role(uid,target,actor='admin'){
 const d=db(uid),before=(await get(d,'users/'+uid)).data();
 const pending=await get(d,'roleRequests/'+uid);
 if(!pending.exists() || pending.data().status!=='PENDING') await put(d,'roleRequests/'+uid,{uid,email:uid+'@example.test',fromRole:before.role,toRole:target,status:'PENDING',createdAt:stamp()});
 const a=db(actor),batch=writeBatch(a);batch.update(doc(a,'users',uid),{role:target,children:{},teamId:'',playerId:'',coachTeamIds:[]});batch.update(doc(a,'roleRequests',uid),{status:'APPROVED'});return batch.commit();
}
async function linkParent(){await issue('coachA');await redeem('parent')}

test('V1 coach scope denies foreign team, player, attendance, training, match and wall; ADMIN global',async()=>{
 for(const uid of ['coachA','legacy','owner']){
  for(const p of ['teams/a','teams/a/players/one','teams/a/trainings/t','teams/a/attendance/x','matches/a','matches/shared','teams/a/posts/p'])await assertSucceeds(get(db(uid),p));
  for(const p of ['teams/b','teams/b/players/one','teams/b/trainings/t','teams/b/attendance/x','matches/b','teams/b/posts/p','users/playerB','users/parent'])await assertFails(get(db(uid),p));
  await assertSucceeds(updateDoc(doc(db(uid),'teams/a/players/one'),{name:'Propio'}));await assertFails(updateDoc(doc(db(uid),'teams/b/players/one'),{name:'Ajeno'}));
 }
 for(const p of ['teams/a','teams/b','users/parent','users/playerB'])await assertSucceeds(get(db('admin'),p));
});
test('V2 queries are scoped: no global coach list; filtered match/children/owner migration queries work',async()=>{
 await assertFails(getDocs(collection(db('coachA'),'teams')));await assertSucceeds(getDocs(collection(db('coachA'),'teams/a/players')));
 await assertFails(getDocs(collection(db('coachA'),'teams/b/players')));
 await assertSucceeds(getDocs(query(collection(db('coachA'),'matches'),where('teamA','==','a'))));await assertFails(getDocs(collection(db('coachA'),'matches')));
 await assertSucceeds(getDocs(query(collection(db('owner'),'teams'),where('ownerId','==','owner'))));
 await assertSucceeds(getDocs(collection(db('admin'),'teams')));await assertFails(getDocs(collection(db('coachA'),'users')));
});
test('V3 ADMIN assigns and revokes scope atomically with audit; no self assignment or extras',async()=>{
 for(const actor of ['coachA','coachB','guest'])await assertFails(updateDoc(doc(db(actor),'users/coachA'),{coachTeamIds:['a','b'],coachScopeBy:actor,coachScopeAt:stamp()}));
 await assertFails(updateDoc(doc(db('admin'),'users/coachA'),{coachTeamIds:['a','b']}));
 await assertFails(updateDoc(doc(db('admin'),'users/coachA'),{coachTeamIds:['a','a'],coachScopeBy:'admin',coachScopeAt:stamp()}));
 await assertFails(updateDoc(doc(db('admin'),'users/coachA'),{coachTeamIds:['b'],coachScopeBy:'admin',coachScopeAt:stamp(),role:'ADMIN'}));
 await assertSucceeds(updateDoc(doc(db('admin'),'users/coachA'),{coachTeamIds:['b'],coachScopeBy:'admin',coachScopeAt:stamp()}));
 await assertFails(get(db('coachA'),'teams/a'));await assertSucceeds(get(db('coachA'),'teams/b'));
});
test('V4 explicit empty scope overrides BOTH legacy owner and teamId; inactive fails closed',async()=>{
 await assertSucceeds(get(db('owner'),'teams/a'));
 await updateDoc(doc(db('admin'),'users/owner'),{coachTeamIds:[],coachScopeBy:'admin',coachScopeAt:stamp()});
 for(const uid of ['owner','empty','inactive','guest'])await assertFails(get(db(uid),'teams/a'));
 await assertFails(get(env.unauthenticatedContext().firestore(),'teams/a'));
});
test('V5 coach cannot create arbitrary teams or transfer ownership; edits own metadata only',async()=>{
 await assertFails(put(db('coachA'),'teams/new',{ownerId:'coachA',active:true}));
 await assertFails(updateDoc(doc(db('coachA'),'teams/a'),{ownerId:'coachA'}));
 await assertSucceeds(updateDoc(doc(db('coachA'),'teams/a'),{name:'Nuevo nombre'}));
 await assertFails(deleteDoc(doc(db('coachA'),'teams/a')));await assertSucceeds(put(db('admin'),'teams/new',{ownerId:'admin',active:true}));
});
test('V6 public signup and role grant never create staff/global scope; ADMIN explicitly assigns coach afterward',async()=>{
 for(const roleName of ['JUGADOR','PADRE'])await assertSucceeds(put(db('signup'+roleName),'users/signup'+roleName,profile('signup'+roleName,roleName)));
 await assertFails(put(db('signup'),'users/signup',profile('signup','ENTRENADOR')));
 await assertFails(put(db('signup2'),'users/signup2',profile('signup2','JUGADOR',{coachTeamIds:['a']})));
 await assertSucceeds(role('guest','ENTRENADOR'));
 await assertFails(get(db('guest'),'teams/a'));
 await updateDoc(doc(db('admin'),'users/guest'),{coachTeamIds:['a'],coachScopeBy:'admin',coachScopeAt:stamp()});await assertSucceeds(get(db('guest'),'teams/a'));
});
test('V7 code issuance requires quota+create atomic; no child name or foreign team',async()=>{
 await assertSucceeds(issue('coachA'));const row=(await get(db('coachA'),'familyCodes/ABCDEFGH2345')).data();assert.equal('name'in row,false);
 await assertFails(put(db('coachA'),'familyCodes/ABCDEFGH2346',{...row,createdAt:stamp()}));
 await assertFails(issue('coachA','ABCDEFGH2347','b'));
 await assertFails(updateDoc(doc(db('coachA'),'familyCodeQuotas/coachA'),{count:0}));
 await assertFails(deleteDoc(doc(db('coachA'),'familyCodeQuotas/coachA')));
});
test('V8 strict per-issuer maximum 20/hour, no reset/forgery and other UID independent',async()=>{
 await seed({'familyCodeQuotas/coachA':{windowStart:ago(1000),count:19,lastCode:'OLD'}});
 await assertSucceeds(issue('coachA'));await assertFails(issue('coachA','ABCDEFGH2346'));
 const a=db('coachA'),b=writeBatch(a);b.set(doc(a,'familyCodes/ABCDEFGH2347'),{teamId:'a',playerId:'one',issuedBy:'coachA',createdAt:stamp(),expiresAt:Timestamp.fromMillis(Date.now()+10000),usedBy:'',usedAt:null});b.set(doc(a,'familyCodeQuotas/coachA'),{windowStart:stamp(),count:1,lastCode:'ABCDEFGH2347'});await assertFails(b.commit());
 await assertSucceeds(issue('admin','ABCDEFGH2348'));
 await seed({'familyCodeQuotas/coachA':{windowStart:ago(3601000),count:20,lastCode:'OLD'}});await assertSucceeds(issue('coachA','ABCDEFGH2349'));
});
test('V9 quota cannot be advanced alone, linked to wrong code or another issuer',async()=>{
 await assertFails(put(db('coachA'),'familyCodeQuotas/coachA',{windowStart:stamp(),count:1,lastCode:'ABCDEFGH2345'}));
 await assertFails(put(db('coachA'),'familyCodeQuotas/coachB',{windowStart:stamp(),count:1,lastCode:'ABCDEFGH2345'}));
 await assertFails(get(db('coachB'),'familyCodeQuotas/coachA'));
});
test('V10 redeem name-free code grants exact child; used receipt scrubbed and can be deleted without losing access',async()=>{
 await linkParent();await assertSucceeds(get(db('parent'),'teams/a/players/one'));await assertFails(get(db('parent'),'teams/a/players/two'));await assertSucceeds(get(db('parent'),'teams/a/attendance/x'));await assertFails(get(db('parent'),'teams/a/attendance/y'));
 const user=(await get(db('parent'),'users/parent')).data();assert.deepEqual(user.children.a.one,{code:'ABCDEFGH2345'});
 await assertSucceeds(deleteDoc(doc(db('parent'),'familyCodes/ABCDEFGH2345')));await assertSucceeds(get(db('parent'),'teams/a/players/one'));
 await assertFails(redeem('other'));
});
test('V11 malformed/expired/inactive/wrong-role and missing writes cannot redeem',async()=>{
 await issue('coachA');await assertFails(redeem('guest'));await assertFails(updateDoc(doc(db('parent'),'users/parent'),{children:{a:{one:{code:'ABCDEFGH2345'}}},lastFamilyCode:'ABCDEFGH2345'}));
 await seed({'users/parent':profile('parent','PADRE',{active:false})});await assertFails(redeem('parent'));
 await seed({'users/parent':profile('parent','PADRE'),'familyCodes/EXPIRED23456':{teamId:'a',playerId:'one',name:'old',issuedBy:'coachA',createdAt:ago(1000),expiresAt:ago(500),usedBy:'',usedAt:null}});
 await assertFails(get(db('parent'),'familyCodes/EXPIRED23456'));
});
test('V12 code cannot smuggle siblings, other teams, legacy links or role',async()=>{
 await issue('coachA');for(const extra of [{role:'ADMIN'},{teamId:'b',playerId:'one'},{coachTeamIds:['b']},{active:false}])await assertFails(redeem('parent','ABCDEFGH2345',extra));
 await assertSucceeds(redeem('parent'));
});
test('V13 two families racing one child yield one winner, no overwrite',async()=>{
 await issue('coachA');await issue('coachA','ABCDEFGH2346');const r=await Promise.allSettled([redeem('parent'),redeem('other','ABCDEFGH2346')]);assert.equal(r.filter(x=>x.status==='fulfilled').length,1);
});
test('V14 ADMIN revoke cuts all future reads and legacy, siblings remain and old code stays dead',async()=>{
 await linkParent();await issue('coachA','ABCDEFGH2346','a','two');await redeem('parent','ABCDEFGH2346');await issue('coachB','ABCDEFGH2347','b','one');await redeem('parent','ABCDEFGH2347');await issue('coachA','ABCDEFGH2348');
 await assertFails(revoke('parent','a','one','coachA'));await assertFails(revoke('parent','a','one','parent'));await assertSucceeds(revoke());
 for(const path of ['teams/a/players/one','teams/a/attendance/x'])await assertFails(get(db('parent'),path));
 for(const path of ['teams/a/players/two','teams/b/players/one','teams/a'])await assertSucceeds(get(db('parent'),path));
 await assertFails(redeem('parent','ABCDEFGH2348'));await issue('coachA','ABCDEFGH2349');await assertSucceeds(redeem('parent','ABCDEFGH2349'));
});
test('V15 no partial revoke, forged audit, extra fields or foreign-owner overwrite',async()=>{
 await linkParent();for(const options of [{skipProfile:true},{skipChild:true},{extra:{role:'ADMIN'}},{child:{name:'X'}},{child:{familyRevokedParentUid:'other'}}])await assertFails(revoke('parent','a','one','admin',options));
 await assertSucceeds(revoke());
});
test('V16 R3 sealed child cannot be deleted/recreated even by ADMIN, nor stamps reset; soft archive works',async()=>{
 await linkParent();await revoke();await assertFails(deleteDoc(doc(db('admin'),'teams/a/players/one')));
 await assertFails(put(db('admin'),'teams/a/players/one',{name:'Recreated',parentUid:null,active:true}));
 await assertFails(updateDoc(doc(db('admin'),'teams/a/players/one'),{familyRevokedAt:deleteField(),familyRevokedParentUid:deleteField()}));
 await assertSucceeds(updateDoc(doc(db('coachA'),'teams/a/players/one'),{active:false}));
});
test('V17 R4 role round-trip clears legacy and cannot regain child by coach assignment',async()=>{
 await linkParent();await revoke();await assertSucceeds(role('parent','JUGADOR'));
 await assertFails(updateDoc(doc(db('coachA'),'users/parent'),{teamId:'a',playerId:'one'}));
 await assertFails(updateDoc(doc(db('admin'),'users/parent'),{teamId:'a',playerId:'one'}));
 await assertFails(get(db('parent'),'teams/a/players/one'));await assertFails(get(db('parent'),'teams/a'));
 await assertSucceeds(role('parent','PADRE'));await issue('coachA','ABCDEFGH2346');await assertSucceeds(redeem('parent','ABCDEFGH2346'));
});
test('V18 legacy pair implanted in a revoked profile never authorizes even team metadata',async()=>{
 await linkParent();await revoke();await env.withSecurityRulesDisabled(async c=>updateDoc(doc(c.firestore(),'users/parent'),{role:'JUGADOR',teamId:'a',playerId:'one'}));
 for(const path of ['teams/a','teams/a/players/one','teams/a/trainings/t','matches/a','teams/a/posts/p'])await assertFails(get(db('parent'),path));
});
test('V19 role change must clear old map, old legacy and old coach scope atomically',async()=>{
 await seed({'users/guest':profile('guest','JUGADOR',{teamId:'a',playerId:'one',children:{a:{one:{name:'old'}}}})});
 await put(db('guest'),'roleRequests/guest',{uid:'guest',email:'guest@example.test',fromRole:'JUGADOR',toRole:'PADRE',status:'PENDING',createdAt:stamp()});
 const a=db('admin'),b=writeBatch(a);b.update(doc(a,'users/guest'),{role:'PADRE',children:{}});b.update(doc(a,'roleRequests/guest'),{status:'APPROVED'});await assertFails(b.commit());
 await assertSucceeds(role('guest','PADRE'));const row=(await get(db('guest'),'users/guest')).data();assert.equal(row.teamId,'');assert.equal(row.playerId,'');assert.deepEqual(row.children,{});
});
test('V20 cleanup deletes only used/expired; active denies; list bounded and team/issuer scoped',async()=>{
 await issue('coachA');await assertFails(deleteDoc(doc(db('coachA'),'familyCodes/ABCDEFGH2345')));await assertFails(deleteDoc(doc(db('admin'),'familyCodes/ABCDEFGH2345')));
 await assertSucceeds(getDocs(query(collection(db('coachA'),'familyCodes'),where('teamId','==','a'),where('issuedBy','==','coachA'),limit(100))));
 await assertFails(getDocs(collection(db('coachA'),'familyCodes')));await assertFails(getDocs(query(collection(db('coachB'),'familyCodes'),where('teamId','==','a'),limit(100))));
 await seed({'familyCodes/EXPIRED23456':{teamId:'a',playerId:'one',issuedBy:'coachA',name:'Viejo',createdAt:ago(5000),expiresAt:ago(1000),usedBy:'',usedAt:null}});
 await assertFails(deleteDoc(doc(db('coachB'),'familyCodes/EXPIRED23456')));await assertSucceeds(deleteDoc(doc(db('coachA'),'familyCodes/EXPIRED23456')));
});
test('V21 ADMIN strips legacy name without changing active code, coach cannot redact or change expiry',async()=>{
 await seed({'familyCodes/ABCDEFGH2345':{teamId:'a',playerId:'one',issuedBy:'coachA',name:'Privado',createdAt:ago(1000),expiresAt:Timestamp.fromMillis(Date.now()+10000),usedBy:'',usedAt:null}});
 await assertFails(updateDoc(doc(db('coachA'),'familyCodes/ABCDEFGH2345'),{name:deleteField()}));
 await assertFails(updateDoc(doc(db('admin'),'familyCodes/ABCDEFGH2345'),{name:deleteField(),expiresAt:Timestamp.fromMillis(Date.now()+3600000)}));
 await assertSucceeds(updateDoc(doc(db('admin'),'familyCodes/ABCDEFGH2345'),{name:deleteField()}));await assertSucceeds(redeem('parent'));assert.equal('name'in(await get(db('parent'),'familyCodes/ABCDEFGH2345')).data(),false);
});
test('V22 legacy named code consumption removes the name in the SAME grant commit',async()=>{
 await seed({'familyCodes/ABCDEFGH2345':{teamId:'a',playerId:'one',issuedBy:'coachA',name:'Privado',createdAt:ago(1000),expiresAt:Timestamp.fromMillis(Date.now()+10000),usedBy:'',usedAt:null}});
 await assertSucceeds(redeem('parent'));assert.equal('name'in(await get(db('parent'),'familyCodes/ABCDEFGH2345')).data(),false);
});
test('V23 self edit, scoped foreign profiles and role requests never expose global families',async()=>{
 await assertFails(updateDoc(doc(db('coachA'),'users/coachA'),{coachTeamIds:['a','b']}));await assertFails(get(db('coachA'),'users/playerB'));await assertFails(get(db('coachA'),'users/parent'));
 await assertSucceeds(get(db('coachA'),'users/playerA'));await assertFails(getDocs(collection(db('coachA'),'roleRequests')));
 await assertFails(updateDoc(doc(db('playerA'),'users/playerA'),{teamId:'b',playerId:'one'}));
});
test('V24 community remains author-scoped and coaches moderate own team only',async()=>{
 const content={text:'Hola',authorUid:'playerA',state:'VISIBLE',createdAt:stamp(),updatedAt:stamp(),updatedBy:'playerA'};
 await assertSucceeds(put(db('playerA'),'teams/a/posts/new',content));await assertFails(put(db('playerA'),'teams/b/posts/new',content));
 await assertSucceeds(updateDoc(doc(db('coachA'),'teams/a/posts/new'),{state:'HIDDEN',updatedAt:stamp(),updatedBy:'coachA'}));
 await assertFails(updateDoc(doc(db('coachB'),'teams/a/posts/new'),{state:'VISIBLE',updatedAt:stamp(),updatedBy:'coachB'}));
});
test('V25 scope removal sends permission-denied to existing listener and blocks later delivery',async()=>{
 const d=db('coachA');let stopped=false;const deliveries=[];let first;const ready=new Promise(r=>first=r);let denied;
 const error=new Promise(r=>denied=r);const off=onSnapshot(doc(d,'teams/a/players/one'),s=>{deliveries.push(s.data()?.name);first()},e=>{stopped=e.code==='permission-denied';denied()});
 try{await ready;await updateDoc(doc(db('admin'),'users/coachA'),{coachTeamIds:[],coachScopeBy:'admin',coachScopeAt:stamp()});await updateDoc(doc(db('admin'),'teams/a/players/one'),{name:'Post-revoke must not arrive'});await Promise.race([error,new Promise((_,r)=>setTimeout(()=>r(Error('no listener deny')),10000))]);assert.equal(stopped,true);await assertFails(get(db('coachA'),'teams/a/players/one'));assert.equal(deliveries.includes('Post-revoke must not arrive'),false)}finally{off()}
});

function eventData(actor='coachA',extra={}) {return {typeId:'goal',typeLabel:'Gol',countsGoal:true,minute:null,teamId:'a',player:'',note:'Registro tardío',voided:false,revision:0,revisionId:'rev-0',createdBy:actor,updatedBy:actor,createdAt:stamp(),updatedAt:stamp(),...extra}}
function eventBatch(actor,match,id,previous,data,version){const d=db(actor),b=writeBatch(d);b.set(doc(d,`matches/${match}/events/${id}`),data);b.set(doc(d,`matches/${match}/events/${id}/revisions/${data.revisionId}`),{actorUid:actor,at:stamp(),previousData:previous,eventData:data});b.update(doc(d,`matches/${match}`),{eventVersion:version,lastEventId:id});return b}
function waitSnapshot(ref,predicate){let off;return new Promise((resolve,reject)=>{const timeout=setTimeout(()=>{off?.();reject(Error('listener timeout'))},10000);off=onSnapshot(ref,s=>{if(predicate(s)){clearTimeout(timeout);off?.();resolve(s)}},e=>{clearTimeout(timeout);off?.();reject(e)})})}
test('V26 live events keep atomic audit, own-team corrections and immutable history',async()=>{
 const ref=doc(db('playerA'),'matches/shared/events/one');let seen=waitSnapshot(ref,s=>s.exists()&&s.data().revision===0);
 await assertSucceeds(eventBatch('coachA','shared','one',{},eventData(),1).commit());assert.equal((await seen).data().minute,null);
 let old=(await get(db('coachA'),'matches/shared/events/one')).data();let next={...old,minute:8,revision:1,revisionId:'rev-1',updatedAt:stamp()};
 await assertFails(eventBatch('coachB','shared','one',old,{...next,updatedBy:'coachB'},2).commit());
 await assertFails(eventBatch('coachA','shared','one',old,{...next,teamId:'b'},2).commit());
 seen=waitSnapshot(ref,s=>s.exists()&&s.data().revision===1);await assertSucceeds(eventBatch('coachA','shared','one',old,next,2).commit());assert.equal((await seen).data().minute,8);
 old=(await get(db('coachA'),'matches/shared/events/one')).data();next={...old,voided:true,revision:2,revisionId:'rev-2',updatedAt:stamp()};
 await assertSucceeds(eventBatch('coachA','shared','one',old,next,3).commit());
 assert.equal((await getDocs(collection(db('playerA'),'matches/shared/events/one/revisions'))).size,3);
 await assertFails(deleteDoc(doc(db('coachA'),'matches/shared/events/one')));
 await assertFails(updateDoc(doc(db('admin'),'matches/shared/events/one/revisions/rev-0'),{actorUid:'admin'}));
 await assertFails(updateDoc(doc(db('coachA'),'matches/shared/events/one'),{note:'Sin historial'}));
 await assertFails(eventBatch('coachA','shared','wrong',{},eventData('coachA',{teamId:'b'}),4).commit());
 await assertFails(eventBatch('playerA','shared','reader',{},eventData('playerA'),4).commit());
 await assertFails(eventBatch('coachA','shared','minute',{},eventData('coachA',{minute:181}),4).commit());
 await assertFails(put(db('coachA'),'matches/shared/events/no-audit',eventData()));
});
test('V27 offline queued event synchronizes with exact same scoped audit contract',async()=>{
 const d=db('coachA');await get(d,'matches/a');await disableNetwork(d);
 // Use this exact SDK instance for the offline write and its snapshot.
 const b=writeBatch(d),data=eventData('coachA',{minute:3});b.set(doc(d,'matches/a/events/queued'),data);b.set(doc(d,'matches/a/events/queued/revisions/rev-0'),{actorUid:'coachA',at:stamp(),previousData:{},eventData:data});b.update(doc(d,'matches/a'),{eventVersion:1,lastEventId:'queued'});
 const local=waitSnapshot(doc(d,'matches/a/events/queued'),s=>s.exists()&&s.metadata.hasPendingWrites);const commit=b.commit();
 try{assert.equal((await local).metadata.hasPendingWrites,true)}finally{await enableNetwork(d)}
 await assertSucceeds(commit);assert.equal((await get(db('playerA'),'matches/a/events/queued')).data().minute,3);
});
test('V28 summary draft remains private and published version binds to own match',async()=>{
 const d=db('coachA'),base={text:'Sólo hechos',sourceHash:'a'.repeat(64),sourceVersion:0,state:'DRAFT',authorUid:'coachA',approvedBy:'',updatedAt:stamp()};
 await assertSucceeds(put(d,'matches/a/aiSummaries/draft',base));
 await assertFails(get(db('playerA'),'matches/a/aiSummaries/draft'));await assertFails(get(db('coachB'),'matches/a/aiSummaries/draft'));
 await assertSucceeds(put(d,'matches/a/aiSummaries/published',{...base,state:'APPROVED',approvedBy:'coachA'}));
 await assertSucceeds(get(db('playerA'),'matches/a/aiSummaries/published'));await assertFails(get(db('coachB'),'matches/a/aiSummaries/published'));
 await assertSucceeds(updateDoc(doc(d,'matches/a'),{status:'in_progress',eventVersion:1,updatedBy:'coachA',updatedAt:stamp()}));
 await assertFails(put(d,'matches/a/aiSummaries/published',{...base,state:'APPROVED',approvedBy:'coachA'}));
 await assertSucceeds(put(d,'matches/a/aiSummaries/published',{...base,sourceVersion:1,state:'APPROVED',approvedBy:'coachA'}));
});
test('V29 quota at nineteen concurrent issues admits only one; actor and scope rechecked',async()=>{
 await seed({'familyCodeQuotas/coachA':{windowStart:ago(1000),count:19,lastCode:'OLD'}});
 const result=await Promise.allSettled([issue('coachA','ABCDEFGH2345'),issue('coachA','ABCDEFGH2346')]);
 assert.equal(result.filter(r=>r.status==='fulfilled').length,1);assert.equal((await get(db('coachA'),'familyCodeQuotas/coachA')).data().count,20);
 await assertFails(issue('coachA','ABCDEFGH2347'));await assertSucceeds(issue('coachB','ABCDEFGH2348','b'));
 await updateDoc(doc(db('admin'),'users/coachB'),{coachTeamIds:[],coachScopeBy:'admin',coachScopeAt:stamp()});await assertFails(issue('coachB','ABCDEFGH2349','b'));
});
test('V30 current assignment cannot be exceeded and coach never gains another scope through role grant',async()=>{
 await assertFails(updateDoc(doc(db('admin'),'users/coachA'),{coachTeamIds:Array.from({length:21},(_,i)=>String(i)),coachScopeBy:'admin',coachScopeAt:stamp()}));
 await seed({'roleRequests/playerB':{uid:'playerB',email:'playerB@example.test',fromRole:'JUGADOR',toRole:'PADRE',status:'PENDING',createdAt:ago(1000)}});
 const d=db('coachA'),b=writeBatch(d);b.update(doc(d,'users/playerB'),{role:'PADRE',children:{},teamId:'',playerId:'',coachTeamIds:[]});b.update(doc(d,'roleRequests/playerB'),{status:'APPROVED'});await assertFails(b.commit());
 await assertSucceeds(get(db('coachB'),'teams/b'));await assertFails(get(db('coachB'),'teams/a'));
});
test('V31 actual create-match batch including twelve catalog entries is authorized only for host scope',async()=>{
 const d=db('coachA'),batch=writeBatch(d);batch.set(doc(d,'matches/new'),{name:'Práctica',teamA:'a',teamB:'b',date:stamp(),status:'not_started',active:true,createdBy:'coachA',eventVersion:0});
 for(let i=0;i<12;i++)batch.set(doc(d,`matches/new/eventTypes/type${i}`),{label:'Tipo '+i,countsGoal:i===2,active:true,updatedBy:'coachA',updatedAt:stamp()});
 await assertSucceeds(batch.commit());assert.equal((await getDocs(collection(db('coachB'),'matches/new/eventTypes'))).size,12);
 await assertFails(put(d,'matches/foreign',{name:'Ajeno',teamA:'b',teamB:'a',date:stamp(),status:'not_started',active:true,createdBy:'coachA',eventVersion:0}));
});
test('V32 creator bootstrap remains verified, cannot grant coach until active ADMIN, then explicit scope',async()=>{
 await seed({'users/creator':profile('creator','JUGADOR'),'users/candidate':profile('candidate','JUGADOR')});
 const creator=env.authenticatedContext('creator',{email:'harontovar@gmail.com',email_verified:true}).firestore(),unverified=env.authenticatedContext('creator',{email:'harontovar@gmail.com',email_verified:false}).firestore();
 await put(db('guest'),'roleRequests/guest',{uid:'guest',email:'guest@example.test',fromRole:'JUGADOR',toRole:'ADMIN',status:'PENDING',createdAt:stamp()});
 function grant(d,uid,target){const b=writeBatch(d);b.update(doc(d,'users',uid),{role:target,children:{},teamId:'',playerId:'',coachTeamIds:[]});b.update(doc(d,'roleRequests',uid),{status:'APPROVED'});return b.commit()}
 await assertFails(grant(unverified,'guest','ADMIN'));await assertSucceeds(grant(creator,'guest','ADMIN'));
 await put(db('candidate'),'roleRequests/candidate',{uid:'candidate',email:'candidate@example.test',fromRole:'JUGADOR',toRole:'ENTRENADOR',status:'PENDING',createdAt:stamp()});
 await assertFails(grant(creator,'candidate','ENTRENADOR'));await assertSucceeds(grant(db('guest'),'candidate','ENTRENADOR'));
 await assertFails(get(db('candidate'),'teams/a'));await assertSucceeds(updateDoc(doc(db('guest'),'users/candidate'),{coachTeamIds:['a'],coachScopeBy:'guest',coachScopeAt:stamp()}));await assertSucceeds(get(db('candidate'),'teams/a'));
});
test('V33 linked family reads exactly its own player attendance and team, never sibling collection',async()=>{
 await linkParent();for(const uid of ['parent','playerA']){
 const d=db(uid);for(const p of ['teams/a','teams/a/players/one','teams/a/trainings/t','teams/a/attendance/x','matches/a'])await assertSucceeds(get(d,p));
 for(const p of ['teams/a/players/two','teams/b/players/one','teams/a/attendance/y','matches/b'])await assertFails(get(d,p));
 await assertFails(getDocs(collection(d,'teams/a/players')));await assertFails(getDocs(collection(d,'teams/a/attendance')));
 await assertSucceeds(getDocs(query(collection(d,'teams/a/attendance'),where('playerId','==','one'))));
 await assertFails(updateDoc(doc(d,'teams/a/players/one'),{name:'Sin staff'}));
 }
});
test('V34 comments reports and reactions preserve scoped moderation and hidden content',async()=>{
 const d=db('playerA'),post='teams/a/posts/p',content={text:'Ánimo',authorUid:'playerA',state:'VISIBLE',createdAt:stamp(),updatedAt:stamp(),updatedBy:'playerA'};
 await assertSucceeds(put(d,post+'/comments/c',content));await assertSucceeds(put(d,post+'/reactions/playerA',{kind:'LIKE',createdAt:stamp()}));
 await assertFails(put(db('playerB'),post+'/reactions/playerA',{kind:'LIKE',createdAt:stamp()}));
 await assertSucceeds(put(d,post+'/reports/playerA',{reason:'Revisar',createdAt:stamp()}));
 await assertSucceeds(get(db('coachA'),post+'/reports/playerA'));await assertFails(get(db('coachB'),post+'/reports/playerA'));
 await assertFails(get(db('parent'),post+'/reports/playerA'));
 await assertSucceeds(updateDoc(doc(db('coachA'),post),{state:'HIDDEN',updatedBy:'coachA',updatedAt:stamp()}));
 await assertFails(get(d,post));await assertFails(get(d,post+'/comments/c'));await assertFails(put(d,post+'/comments/new',content));await assertSucceeds(get(db('coachA'),post));
});
test('V35 deleting eligible receipts never refunds quota and untrusted expiry cannot delete an active code',async()=>{
 await seed({'familyCodeQuotas/coachA':{windowStart:ago(1000),count:20,lastCode:'ABCDEFGH2345'},'familyCodes/ABCDEFGH2345':{teamId:'a',playerId:'one',issuedBy:'coachA',createdAt:ago(2000),expiresAt:ago(1000),usedBy:'',usedAt:null}});
 await assertSucceeds(deleteDoc(doc(db('coachA'),'familyCodes/ABCDEFGH2345')));await assertFails(issue('coachA','ABCDEFGH2346'));
 await assertFails(deleteDoc(doc(db('coachA'),'familyCodeQuotas/coachA')));
 await seed({'familyCodes/ABCDEFGH2347':{teamId:'a',playerId:'one',issuedBy:'coachA',createdAt:ago(1000),expiresAt:Timestamp.fromMillis(Date.now()+100000),usedBy:'',usedAt:null}});
 await assertFails(updateDoc(doc(db('coachA'),'familyCodes/ABCDEFGH2347'),{expiresAt:ago(1000)}));await assertFails(deleteDoc(doc(db('coachA'),'familyCodes/ABCDEFGH2347')));
});
test('V36 boundary assignment of twenty teams works, twenty-one denied and scope revocation overrides legacy',async()=>{
 await seed({'teams/scope19':{ownerId:'admin',active:true}});const ids=Array.from({length:20},(_,i)=>'scope'+i);
 await assertSucceeds(updateDoc(doc(db('admin'),'users/legacy'),{coachTeamIds:ids,coachScopeBy:'admin',coachScopeAt:stamp()}));
 await assertSucceeds(get(db('legacy'),'teams/scope19'));await assertFails(get(db('legacy'),'teams/a'));
 await assertFails(updateDoc(doc(db('admin'),'users/legacy'),{coachTeamIds:[...ids,'twenty-first'],coachScopeBy:'admin',coachScopeAt:stamp()}));
});
test('V37 old named family grants still read and can be revoked; new client consumes old codes without new names',async()=>{
 await seed({'users/parent':profile('parent','PADRE',{children:{a:{one:{name:'Legacy minor',code:'OLD'}}}}),'teams/a/players/one':{name:'Menor A',active:true,parentUid:'parent'}});
 await assertSucceeds(get(db('parent'),'teams/a/players/one'));await assertSucceeds(revoke());await assertFails(get(db('parent'),'teams/a/players/one'));
 await seed({'familyCodes/ABCDEFGH2345':{teamId:'a',playerId:'one',name:'Menor A',issuedBy:'coachA',createdAt:Timestamp.fromMillis(Date.now()+5),expiresAt:Timestamp.fromMillis(Date.now()+100000),usedBy:'',usedAt:null}});
 await new Promise(r=>setTimeout(r,10));await assertSucceeds(redeem('parent'));
 assert.deepEqual((await get(db('parent'),'users/parent')).data().children.a.one,{code:'ABCDEFGH2345'});
});
