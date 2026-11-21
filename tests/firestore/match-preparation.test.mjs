// Synthetic fixtures only. Run against an explicitly supplied LOCAL emulator.
// SPORTPRO_RULES_FILE must point to a candidate containing the fragment.
import {test, before, beforeEach, after} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {initializeTestEnvironment} from '@firebase/rules-unit-testing';
import {doc, collection, getDocFromServer, getDocs, setDoc, updateDoc, deleteDoc,
  writeBatch, runTransaction, serverTimestamp, Timestamp} from 'firebase/firestore';

const projectId = 'demo-sportpro-match-preparation-2448';
const path = 'matches/demo-match/squads/demo-a';
const playerId = n => 'demo-player-' + n;
const row = (n, extra={}) => ({name:'Jugador demo '+n, invited:true,
  response:'PENDING', respondedBy:'', respondedAt:null, ...extra});
const confirmed = n => row(n,{response:'CONFIRMED',respondedBy:'demo-player-user',respondedAt:Timestamp.fromMillis(1000)});
const initial = (extra={}) => ({teamId:'demo-a', revision:1, lineupRevision:-1,
  roster:{[playerId(1)]:row(1)}, starters:[], substitutes:[], positions:{},
  lastPlayerId:playerId(1), updatedBy:'demo-coach', updatedAt:serverTimestamp(), ...extra});
let env;
const db = uid => env.authenticatedContext(uid,{email:uid+'@example.test',email_verified:true}).firestore();
const ref = (d,p=path) => doc(d,p);
const deny = promise => assert.rejects(promise,e=>e.code==='permission-denied');
async function seed(items) {
  await env.withSecurityRulesDisabled(async c=>{
    const b=writeBatch(c.firestore());
    for(const [p,v] of Object.entries(items)) b.set(ref(c.firestore(),p),v);
    await b.commit();
  });
}
async function stored() {
  let result;
  await env.withSecurityRulesDisabled(async c=>{result=(await getDocFromServer(ref(c.firestore()))).data();});
  return result;
}
async function create(actor='demo-coach', extra={}, target=path) {
  return setDoc(ref(db(actor),target),initial({updatedBy:actor,...extra}));
}
async function change(actor, changes={}, rev) {
  const previous=await stored();
  return updateDoc(ref(db(actor)),{revision:rev??previous.revision+1,
    updatedBy:actor,updatedAt:serverTimestamp(),...changes});
}
async function invite(n, invited=true, extra={}) {
  const old=await stored();
  return change('demo-coach',{roster:{...old.roster,[playerId(n)]:row(n,{invited})},lastPlayerId:playerId(n),...extra});
}
async function respond(actor='demo-player-user', response='CONFIRMED', n=1, extra={}) {
  const old=await stored();
  return change(actor,{roster:{...old.roster,[playerId(n)]:{
    ...old.roster[playerId(n)],response,respondedBy:actor,respondedAt:serverTimestamp(),recordedByCoach:false,...extra}},lastPlayerId:playerId(n)});
}
async function lineup(starters=[playerId(1)],substitutes=[],extra={},actor='demo-coach') {
  const old=await stored();
  return change(actor,{starters,substitutes,
    positions:Object.fromEntries([...starters,...substitutes].map(id=>[id,'DELANTERO'])),
    lineupRevision:old.revision+1,...extra});
}
async function ready(count=23) {
  await seed({[path]:initial({roster:Object.fromEntries(Array.from({length:count},(_,i)=>[playerId(i+1),confirmed(i+1)]))})});
}

before(async()=>{
  assert.match(process.env.FIRESTORE_EMULATOR_HOST??'',/^127\.0\.0\.1:\d+$/,
    'Only an explicitly configured localhost emulator is permitted');
  assert.ok(process.env.SPORTPRO_RULES_FILE,'Supply candidate rules; never loads production');
  const port=Number(process.env.FIRESTORE_EMULATOR_HOST.split(':')[1]);
  env=await initializeTestEnvironment({projectId,firestore:{host:'127.0.0.1',port,
    rules:readFileSync(process.env.SPORTPRO_RULES_FILE,'utf8')}});
});
beforeEach(async()=>{
  await env.clearFirestore();
  const fixtures={
    'users/demo-coach':{uid:'demo-coach',role:'ENTRENADOR',active:true,coachTeamIds:['demo-a']},
    'users/demo-rival-coach':{uid:'demo-rival-coach',role:'ENTRENADOR',active:true,coachTeamIds:['demo-b']},
    'users/demo-empty-coach':{uid:'demo-empty-coach',role:'ENTRENADOR',active:true,coachTeamIds:[]},
    'users/demo-admin':{uid:'demo-admin',role:'ADMIN',active:true},
    'users/demo-player-user':{uid:'demo-player-user',role:'JUGADOR',active:true,teamId:'demo-a',playerId:playerId(1)},
    'users/demo-other-player':{uid:'demo-other-player',role:'JUGADOR',active:true,teamId:'demo-a',playerId:playerId(2)},
    'users/demo-rival-player':{uid:'demo-rival-player',role:'JUGADOR',active:true,teamId:'demo-b',playerId:'demo-rival'},
    'users/demo-parent':{uid:'demo-parent',role:'PADRE',active:true,children:{'demo-a':{[playerId(1)]:{code:'DEMO'}}}},
    'users/demo-foreign-parent':{uid:'demo-foreign-parent',role:'PADRE',active:true,children:{'demo-a':{[playerId(2)]:{code:'DEMO'}}}},
    'teams/demo-a':{name:'Equipo demo A',active:true},
    'teams/demo-b':{name:'Equipo demo B',active:true},
    'teams/demo-c':{name:'Equipo demo C',active:true},
    'teams/demo-b/players/demo-rival':{name:'Jugador demo rival',teamId:'demo-b',active:true},
    'matches/demo-match':{teamA:'demo-a',teamB:'demo-b',name:'Partido demo',active:true,eventVersion:0}
  };
  for(let n=1;n<=41;n++) fixtures['teams/demo-a/players/'+playerId(n)]={
    id:playerId(n),teamId:'demo-a',name:'Jugador demo '+n,active:true};
  await seed(fixtures);
});
after(async()=>{if(env)await env.cleanup();});

test('01 full lifecycle: invite, player/tutor reply, lineup, stale revision after withdrawal',async()=>{
  await create(); await respond(); await lineup();
  let x=await stored();assert.equal(x.lineupRevision,x.revision);
  await invite(1,false);x=await stored();
  assert.equal(x.roster[playerId(1)].response,'PENDING');
  assert.equal(x.roster[playerId(1)].respondedAt,null);
  assert.ok(x.lineupRevision<x.revision);assert.deepEqual(x.starters,[playerId(1)]);
  await deny(respond());await deny(lineup());await invite(1,true);
  await respond('demo-parent');await lineup();x=await stored();
  assert.equal(x.roster[playerId(1)].respondedBy,'demo-parent');assert.equal(x.lineupRevision,x.revision);
});
for(const actor of ['demo-rival-coach','demo-empty-coach','demo-player-user','demo-parent'])
  test('02 creation denies '+actor,async()=>{await deny(create(actor));await create();});
test('03 creation exact keys, row source, initial state and match scope',async()=>{
  for(const extra of [{teamId:'demo-b'},{revision:2},{lineupRevision:1},{secret:'x'},
    {roster:{}},{roster:{[playerId(1)]:row(1),[playerId(2)]:row(2)}},
    {roster:{[playerId(1)]:row(1,{name:''})}},
    {roster:{[playerId(1)]:row(1,{name:'x'.repeat(121)})}},
    {roster:{[playerId(1)]:row(1,{invited:false})}},
    {roster:{[playerId(1)]:row(1,{response:'CONFIRMED'})}},
    {roster:{[playerId(1)]:row(1,{respondedBy:'demo-player-user'})}},
    {roster:{[playerId(1)]:row(1,{respondedAt:Timestamp.fromMillis(1)})}},
    {roster:{[playerId(1)]:row(1,{phone:'000'})}}, {starters:[playerId(1)]},
    {positions:{[playerId(1)]:'PORTERO'}},{updatedBy:'demo-admin'}, {updatedAt:Timestamp.fromMillis(1)}])
    await deny(create('demo-coach',extra));
  await deny(create('demo-admin',{teamId:'demo-c'},'matches/demo-match/squads/demo-c'));
  await deny(create('demo-admin',{},'matches/demo-missing/squads/demo-a'));
  // Authorized name projection may be newer than the legacy player-row name.
  await create('demo-coach',{roster:{[playerId(1)]:row(1,{name:'Jugador demo proyectado'})}});
});
test('04 absent, inactive and foreign team players cannot enter roster',async()=>{
  await seed({['teams/demo-a/players/'+playerId(1)]:{name:'Jugador demo 1',teamId:'demo-a',active:false}});
  await deny(create());await seed({['teams/demo-a/players/'+playerId(1)]:{name:'Jugador demo 1',teamId:'demo-a',active:true}});
  await create();
  await deny(invite(99));
  await deny(change('demo-coach',{lastPlayerId:'demo-rival',roster:{...(await stored()).roster,'demo-rival':row(1,{name:'Jugador demo rival'})}}));
  await seed({['teams/demo-a/players/'+playerId(2)]:{name:'Jugador demo 2',teamId:'demo-b',active:true}});
  await deny(invite(2));await invite(3);
});
test('04b withdrawal remains possible for a now-inactive player; reinviting remains denied',async()=>{
  await create();await respond();
  await seed({['teams/demo-a/players/'+playerId(1)]:{name:'Jugador demo 1',teamId:'demo-a',active:false}});
  await invite(1,false);await deny(invite(1,true));
  assert.equal((await stored()).roster[playerId(1)].response,'PENDING');
  await deny(invite(2,false));
});
test('05 roster changes one row, never deletes, no reply forgery or resetting same invitation',async()=>{
  await create();await respond();
  await deny(invite(1,true));
  await deny(change('demo-coach',{roster:{[playerId(1)]:row(1,{invited:false}),[playerId(2)]:row(2)},lastPlayerId:playerId(2)}));
  await deny(change('demo-coach',{roster:{},lastPlayerId:playerId(1)}));
  await deny(invite(2,true,{lastPlayerId:playerId(3)}));
  for(const actor of ['demo-coach','demo-rival-coach','demo-admin','demo-other-player','demo-foreign-parent'])
    await deny(respond(actor));
  await invite(2);
});
test('06 response cannot change name/invitation, inject fields, actor/time or PENDING',async()=>{
  await create();
  for(const extra of [{name:'Inventado'},{invited:false},{respondedBy:'demo-parent'},
    {respondedAt:null},{respondedAt:Timestamp.fromMillis(1)},{phone:'000'}]) await deny(respond('demo-player-user','CONFIRMED',1,extra));
  await deny(respond('demo-player-user','PENDING'));
  await respond('demo-player-user','DECLINED');await respond('demo-parent','CONFIRMED');
});
test('07 metadata, revision, immutable team and stale client rejected atomically',async()=>{
  await create();
  for(const changes of [{revision:1},{revision:3},{revision:2.5},{teamId:'demo-b'},
    {updatedBy:'demo-admin'},{updatedAt:Timestamp.fromMillis(1)},{lineupRevision:0},{secret:'x'}])
    await deny(invite(2,true,changes));
  await invite(2);const old=await stored();
  await deny(change('demo-coach',{roster:{...old.roster,[playerId(3)]:row(3)},lastPlayerId:playerId(3)},2));
  assert.equal((await stored()).revision,2);await invite(3);
});
test('08 full 11+12 lineup validates all 23 slots without player reads',async()=>{
  await ready();await lineup(Array.from({length:11},(_,i)=>playerId(i+1)),Array.from({length:12},(_,i)=>playerId(i+12)));
  const x=await stored();assert.equal(x.starters.length,11);assert.equal(x.substitutes.length,12);assert.equal(x.lineupRevision,2);
  // Last slot must be checked, not only the starting eleven.
  await ready();const x2=await stored();x2.roster[playerId(23)]=row(23);await seed({[path]:x2});
  await deny(lineup(Array.from({length:11},(_,i)=>playerId(i+1)),Array.from({length:12},(_,i)=>playerId(i+12))));
});
for(const bad of ['empty','tooManyStarters','tooManySubs','duplicateStarters','duplicateSubs','overlap','missing','pending','declined','withdrawn','positionMissing','positionExtra','positionInvalid','nonString','lineupRevision','rosterChange','lastPlayerId'])
test('09 invalid lineup '+bad,async()=>{
  await ready(24);let starters=[playerId(1)],subs=[playerId(2)],extra={};
  if(bad==='empty') starters=[];
  if(bad==='tooManyStarters') {starters=Array.from({length:12},(_,i)=>playerId(i+1));subs=[];}
  if(bad==='tooManySubs') subs=Array.from({length:13},(_,i)=>playerId(i+2));
  if(bad==='duplicateStarters') starters=[playerId(1),playerId(1)];
  if(bad==='duplicateSubs') subs=[playerId(2),playerId(2)];
  if(bad==='overlap') subs=[playerId(1)];
  if(bad==='missing') starters=['demo-missing'];
  if(['pending','declined','withdrawn'].includes(bad)) {
    const x=await stored();x.roster[playerId(1)]={...x.roster[playerId(1)],
      ...(bad==='withdrawn'?{invited:false}:{response:bad==='pending'?'PENDING':'DECLINED'})};await seed({[path]:x});
  }
  if(bad==='positionMissing') extra.positions={[playerId(1)]:'PORTERO'};
  if(bad==='positionExtra') extra.positions={[playerId(1)]:'PORTERO',[playerId(2)]:'DELANTERO',[playerId(3)]:'EXTREMO'};
  if(bad==='positionInvalid') extra.positions={[playerId(1)]:'MAGICO',[playerId(2)]:'DELANTERO'};
  if(bad==='nonString') starters=[7];
  if(bad==='lineupRevision') extra.lineupRevision=1;
  if(bad==='rosterChange') extra.roster={...(await stored()).roster,[playerId(1)]:row(1,{invited:false})};
  if(bad==='lastPlayerId') extra.lastPlayerId=playerId(2);
  await deny(lineup(starters,subs,extra));await ready();await lineup();
});
test('10 lineup write only own staff; no player, tutor or rival write',async()=>{
  await ready();
  for(const actor of ['demo-player-user','demo-parent','demo-rival-coach','demo-empty-coach']) await deny(lineup([playerId(1)],[],{},actor));
  await lineup();
});
test('11 read own squad only; no rival, unrelated, anonymous, collection listing or delete',async()=>{
  await create();
  await seed({'matches/demo-match/squads/demo-b':initial({teamId:'demo-b'}),
    'matches/demo-match/squads/demo-c':initial({teamId:'demo-c'})});
  for(const actor of ['demo-coach','demo-player-user','demo-other-player','demo-parent','demo-admin'])
    assert.equal((await getDocFromServer(ref(db(actor)))).exists(),true);
  for(const actor of ['demo-rival-coach','demo-rival-player','demo-empty-coach']) await deny(getDocFromServer(ref(db(actor))));
  await deny(getDocFromServer(ref(env.unauthenticatedContext().firestore())));
  await deny(getDocFromServer(ref(db('demo-coach'),'matches/demo-match/squads/demo-b')));
  await deny(getDocFromServer(ref(db('demo-admin'),'matches/demo-match/squads/demo-c')));
  await deny(getDocs(collection(db('demo-coach'),'matches/demo-match/squads')));
  for(const actor of ['demo-admin','demo-coach','demo-player-user']) await deny(deleteDoc(ref(db(actor))));
});
test('12 inactive account loses access and restored account succeeds',async()=>{
  await create();await seed({'users/demo-coach':{role:'ENTRENADOR',active:false,coachTeamIds:['demo-a']}});
  await deny(invite(2));await deny(getDocFromServer(ref(db('demo-coach'))));
  await seed({'users/demo-coach':{role:'ENTRENADOR',active:true,coachTeamIds:['demo-a']}});await invite(2);
});
test('13 forty roster entries include withdrawn rows; forty-first cannot enter',async()=>{
  await ready(40);await invite(1,false);await deny(invite(41));
  assert.equal(Object.keys((await stored()).roster).length,40);
  await invite(1,true);
});
test('14 minimal one starter and zero substitutes; absent pre-read allowed for own staff',async()=>{
  assert.equal((await getDocFromServer(ref(db('demo-coach')))).exists(),false);
  const d=db('demo-coach');await runTransaction(d,async tx=>{
    assert.equal((await tx.get(ref(d))).exists(),false);tx.set(ref(d),initial());
  });await respond();await lineup();assert.deepEqual((await stored()).substitutes,[]);
});
test('15 two stale writers at the same revision yield exactly one winner',async()=>{
  await create();const base=await stored();
  const results=await Promise.allSettled([2,3].map(n=>updateDoc(ref(db('demo-coach')),
    {revision:2,updatedBy:'demo-coach',updatedAt:serverTimestamp(),lastPlayerId:playerId(n),
      roster:{...base.roster,[playerId(n)]:row(n)}})));
  assert.equal(results.filter(x=>x.status==='fulfilled').length,1);
  const failure=results.find(x=>x.status==='rejected');assert.equal(failure.reason.code,'permission-denied');
  const x=await stored();assert.equal(x.revision,2);assert.equal(Object.keys(x.roster).length,2);
});


test('16 own coach records player response with actual author then lineup works on one account',async()=>{
  await create();await respond('demo-coach','CONFIRMED',1,{recordedByCoach:true});
  let x=await stored();assert.equal(x.roster[playerId(1)].respondedBy,'demo-coach');
  assert.equal(x.roster[playerId(1)].recordedByCoach,true);assert.ok(x.roster[playerId(1)].respondedAt instanceof Timestamp);
  await lineup();assert.equal((await stored()).lineupRevision,(await stored()).revision);
  await respond('demo-coach','DECLINED',1,{recordedByCoach:true});await deny(lineup());
  await respond('demo-parent','CONFIRMED');x=await stored();
  assert.equal(x.roster[playerId(1)].recordedByCoach,false);assert.equal(x.roster[playerId(1)].respondedBy,'demo-parent');
  await invite(1,false);assert.equal((await stored()).roster[playerId(1)].recordedByCoach??false,false);
});
for(const actor of ['demo-rival-coach','demo-empty-coach','demo-player-user','demo-parent','demo-other-player','demo-foreign-parent'])
test('17 coach attribution refused without own staff scope '+actor,async()=>{
  await create();await deny(respond(actor,'CONFIRMED',1,{recordedByCoach:true}));
  assert.equal((await stored()).roster[playerId(1)].response,'PENDING');
});
test('18 delegated response keeps name actor timestamp row boundaries and invited guard',async()=>{
  await create();await invite(2);
  for(const extra of [{recordedByCoach:true,name:'Falso'},{recordedByCoach:true,respondedBy:'demo-player-user'},
    {recordedByCoach:true,respondedAt:Timestamp.fromMillis(1)},{recordedByCoach:true,phone:'000'},
    {recordedByCoach:'true'}]) await deny(respond('demo-coach','CONFIRMED',1,extra));
  const old=await stored();await deny(change('demo-coach',{lastPlayerId:playerId(1),roster:{...old.roster,
    [playerId(1)]:{...old.roster[playerId(1)],response:'CONFIRMED',respondedBy:'demo-coach',respondedAt:serverTimestamp(),recordedByCoach:true},
    [playerId(2)]:{...old.roster[playerId(2)],response:'CONFIRMED',respondedBy:'demo-coach',respondedAt:serverTimestamp(),recordedByCoach:true}}}));
  await invite(1,false);await deny(respond('demo-coach','CONFIRMED',1,{recordedByCoach:true}));
});
test('19 inactive coach cannot register; legacy five-field rows remain usable',async()=>{
  await create();assert.equal((await stored()).roster[playerId(1)].recordedByCoach,undefined);
  await seed({'users/demo-coach':{role:'ENTRENADOR',active:false,coachTeamIds:['demo-a']}});
  await deny(respond('demo-coach','CONFIRMED',1,{recordedByCoach:true}));
  await seed({'users/demo-coach':{role:'ENTRENADOR',active:true,coachTeamIds:['demo-a']}});
  await respond('demo-coach','CONFIRMED',1,{recordedByCoach:true});
  assert.equal((await stored()).roster[playerId(1)].recordedByCoach,true);
});
