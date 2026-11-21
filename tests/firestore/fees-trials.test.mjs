import {test,before,beforeEach,after} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import {initializeTestEnvironment,assertFails,assertSucceeds} from '@firebase/rules-unit-testing';
import {doc,collection,query,where,getDocFromServer,getDocs,setDoc,updateDoc,deleteDoc,writeBatch,runTransaction,serverTimestamp,Timestamp} from 'firebase/firestore';
const projectId='demo-sportpro-fees-trials';
const host=process.env.FIRESTORE_EMULATOR_HOST;
assert.equal(host,'127.0.0.1:8193','Only the explicit local emulator is authorized');
assert.ok(process.env.SPORTPRO_RULES_FILE,'An explicit local combined rules file is required');
let env; const records=[];
const db=(uid)=>env.authenticatedContext(uid,{email:uid+'@example.test',email_verified:true}).firestore();
const ref=(d,p)=>doc(d,p);
const feePath=(player='p1',period='2026-11',team='a')=>`teams/${team}/players/${player}/simulatedFees/${period}`;
const fee=(changes={})=>({teamId:'a',playerId:'p1',period:'2026-11',status:'PENDING',simulated:true,revision:1,createdBy:'admin',createdAt:serverTimestamp(),updatedBy:'admin',updatedAt:serverTimestamp(),...changes});
const trial=(changes={})=>({teamId:'a',title:'Prueba deportiva adulta',category:'SENIOR',position:'ANY',dateMillis:Date.UTC(2026,11,5),location:'Campo de demostración',description:'Prueba ficticia académica sin contactos personales.',state:'PENDING',createdBy:'coach',createdAt:serverTimestamp(),updatedBy:'coach',updatedAt:serverTimestamp(),approvedBy:'',approvedAt:null,revision:1,...changes});
async function seed(rows){await env.withSecurityRulesDisabled(async ctx=>{const b=writeBatch(ctx.firestore());for(const [p,v] of Object.entries(rows))b.set(ref(ctx.firestore(),p),v);await b.commit()});}
async function raw(p){let value;await env.withSecurityRulesDisabled(async ctx=>{const s=await getDocFromServer(ref(ctx.firestore(),p));value=s.exists()?s.data():null});return value;}
async function publish(id='t1'){await updateDoc(ref(db('admin'),'openTrials/'+id),{state:'PUBLISHED',updatedBy:'admin',updatedAt:serverTimestamp(),approvedBy:'admin',approvedAt:serverTimestamp(),revision:2});}
before(async()=>{env=await initializeTestEnvironment({projectId,firestore:{host:'127.0.0.1',port:8193,rules:readFileSync(process.env.SPORTPRO_RULES_FILE,'utf8')}})});
beforeEach(async()=>{await env.clearFirestore();await seed({
 'users/admin':{role:'ADMIN',active:true},'users/coach':{role:'ENTRENADOR',active:true,coachTeamIds:['a']},'users/otherCoach':{role:'ENTRENADOR',active:true,coachTeamIds:['b']},
 'users/player':{role:'JUGADOR',active:true,teamId:'a',playerId:'p1'},'users/other':{role:'JUGADOR',active:true,teamId:'a',playerId:'p2'},
 'users/parent':{role:'PADRE',active:true,children:{a:{p1:{}}}},'users/stranger':{role:'PADRE',active:true,children:{b:{q1:{}}}},
 'users/inactive':{role:'ADMIN',active:false},'teams/a':{active:true},'teams/b':{active:true},
 'teams/a/players/p1':{name:'Persona ficticia uno',active:true},'teams/a/players/p2':{name:'Persona ficticia dos',active:true},'teams/b/players/q1':{name:'Persona ficticia tres',active:true}
})});
after(async()=>{if(env)await env.cleanup();if(process.env.SPORTPRO_EVIDENCE_FILE)writeFileSync(process.env.SPORTPRO_EVIDENCE_FILE,JSON.stringify({utc:new Date().toISOString(),projectId,scope:'local SDK and emulator, synthetic adult fixtures; not Android UI or production',records},null,2)+'\n')});

test('F01 admin records pending then paid simulated; own player and parent see same status',async()=>{
 const r=ref(db('admin'),feePath());await assertSucceeds(setDoc(r,fee()));
 await assertSucceeds(updateDoc(r,{status:'PAID',revision:2,updatedBy:'admin',updatedAt:serverTimestamp()}));
 for(const actor of ['player','parent']){const s=await assertSucceeds(getDocFromServer(ref(db(actor),feePath())));assert.equal(s.data().status,'PAID');assert.equal(s.data().simulated,true)}
 records.push({case:'F01',status:'PAID',simulated:true,readers:['player','parent']});
});
test('F02 missing fee is absent, not an invented debt',async()=>{const s=await assertSucceeds(getDocFromServer(ref(db('player'),feePath())));assert.equal(s.exists(),false)});
for(const actor of ['coach','otherCoach','player','parent','other','inactive'])test('F03 nonadmin cannot create fee: '+actor,async()=>{await assertFails(setDoc(ref(db(actor),feePath()),fee({createdBy:actor,updatedBy:actor})));await assertSucceeds(setDoc(ref(db('admin'),feePath()),fee()))});
for(const actor of ['coach','otherCoach','other','stranger','inactive'])test('F04 foreign financial read denied: '+actor,async()=>{await setDoc(ref(db('admin'),feePath()),fee());await assertFails(getDocFromServer(ref(db(actor),feePath())))});
test('F05 anonymous fee access denied',async()=>{await assertFails(getDocFromServer(ref(env.unauthenticatedContext().firestore(),feePath())))});
for(const changes of [{period:'2026-12'},{playerId:'p2'},{teamId:'b'},{status:'CHARGED'},{simulated:false},{amount:100},{paymentToken:'fictitious'},{createdBy:'coach'},{updatedBy:'coach'},{createdAt:Timestamp.fromMillis(0)},{updatedAt:Timestamp.fromMillis(0)},{revision:2}])test('F06 schema and actor refutator '+JSON.stringify(changes),async()=>{await assertFails(setDoc(ref(db('admin'),feePath()),fee(changes)))});
for(const period of ['2026-00','2026-13','26-11','2026-1','0000-11'])test('F07 invalid period '+period,async()=>{await assertFails(setDoc(ref(db('admin'),feePath('p1',period)),fee({period})))});
test('F08 old revision, immutable trace and deletion denied; family cannot edit',async()=>{
 const r=ref(db('admin'),feePath());await setDoc(r,fee());
 await assertFails(updateDoc(r,{status:'PAID',updatedAt:serverTimestamp(),revision:1}));
 await assertFails(updateDoc(r,{createdBy:'coach',updatedAt:serverTimestamp(),revision:2}));
 await assertFails(updateDoc(ref(db('player'),feePath()),{status:'PAID',updatedBy:'player',updatedAt:serverTimestamp(),revision:2}));
 await assertFails(deleteDoc(r));
});
test('F09 two edits from same revision yield one commit; no silent overwrite',async()=>{
 await setDoc(ref(db('admin'),feePath()),fee());
 const change=async status=>{const d=db('admin');return runTransaction(d,async tx=>{const r=ref(d,feePath());const s=await tx.get(r);if(s.data().revision!==1)throw new Error('conflict');tx.update(r,{status,revision:2,updatedBy:'admin',updatedAt:serverTimestamp()})})};
 const r=await Promise.allSettled([change('PAID'),change('EXEMPT')]);assert.equal(r.filter(x=>x.status==='fulfilled').length,1);assert.equal((await raw(feePath())).revision,2);records.push({case:'F09',commits:1,revision:2});
});

test('T01 scoped coach proposes; admin publishes; registered reader sees published',async()=>{
 await assertSucceeds(setDoc(ref(db('coach'),'openTrials/t1'),trial()));
 await assertFails(getDocFromServer(ref(db('player'),'openTrials/t1')));
 await publish();const s=await assertSucceeds(getDocFromServer(ref(db('player'),'openTrials/t1')));assert.equal(s.data().state,'PUBLISHED');assert.equal(s.data().approvedBy,'admin');
 const list=await assertSucceeds(getDocs(query(collection(db('player'),'openTrials'),where('state','==','PUBLISHED'))));assert.equal(list.size,1);
 records.push({case:'T01',pendingPrivate:true,published:1});
});
for(const actor of ['otherCoach','player','parent','inactive'])test('T02 unauthorized proposal '+actor,async()=>{await assertFails(setDoc(ref(db(actor),'openTrials/t1'),trial({createdBy:actor,updatedBy:actor})))});
test('T03 coach cannot self-publish or impersonate admin',async()=>{await setDoc(ref(db('coach'),'openTrials/t1'),trial());await assertFails(updateDoc(ref(db('coach'),'openTrials/t1'),{state:'PUBLISHED',approvedBy:'coach',approvedAt:serverTimestamp(),updatedBy:'coach',updatedAt:serverTimestamp(),revision:2}));await assertFails(setDoc(ref(db('coach'),'openTrials/fake'),trial({state:'PUBLISHED',approvedBy:'admin',approvedAt:serverTimestamp()})))});
test('T04 anonymous and inactive cannot see published',async()=>{await setDoc(ref(db('coach'),'openTrials/t1'),trial());await publish();for(const d of [db('inactive'),env.unauthenticatedContext().firestore()])await assertFails(getDocFromServer(ref(d,'openTrials/t1')))});
test('T05 no unfiltered common-user query or foreign pending reads',async()=>{await setDoc(ref(db('coach'),'openTrials/t1'),trial());await assertFails(getDocs(collection(db('player'),'openTrials')));await assertFails(getDocFromServer(ref(db('otherCoach'),'openTrials/t1')));await assertSucceeds(getDocs(query(collection(db('admin'),'openTrials'),where('state','==','PENDING'))))});
for(const changes of [{phone:'000000'},{createdBy:'admin'},{updatedBy:'admin'},{revision:2},{approvedBy:'coach'},{approvedAt:serverTimestamp()},{category:'OTHER'},{position:'GOALKEEPER'},{dateMillis:Date.UTC(2026,11,5)+1},{dateMillis:-1},{title:''},{location:''},{description:''},{description:'Escribe a hola@example.test'},{description:'Llama al 999 888 777'},{description:'Visita https://example.test'}])test('T06 proposal shape denied '+Object.keys(changes).join(','),async()=>{await assertFails(setDoc(ref(db('coach'),'openTrials/t1'),trial(changes)))});
test('T07 revision conflict, stale approval and deletion denied',async()=>{await setDoc(ref(db('coach'),'openTrials/t1'),trial());await publish();await assertFails(updateDoc(ref(db('admin'),'openTrials/t1'),{state:'PUBLISHED',updatedBy:'admin',updatedAt:serverTimestamp(),approvedBy:'admin',approvedAt:serverTimestamp(),revision:2}));await assertFails(deleteDoc(ref(db('admin'),'openTrials/t1')))});
test('T08 edit published proposal returns to pending and removes approval',async()=>{await setDoc(ref(db('coach'),'openTrials/t1'),trial());await publish();await assertSucceeds(updateDoc(ref(db('coach'),'openTrials/t1'),{title:'Prueba revisada',state:'PENDING',approvedBy:'',approvedAt:null,updatedBy:'coach',updatedAt:serverTimestamp(),revision:3}));await assertFails(getDocFromServer(ref(db('player'),'openTrials/t1')));assert.equal((await raw('openTrials/t1')).state,'PENDING')});
