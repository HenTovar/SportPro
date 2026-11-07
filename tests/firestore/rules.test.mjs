import assert from 'node:assert/strict';
import { after, before, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, doc, getDoc, getDocs, query, setDoc, updateDoc, deleteDoc, where, serverTimestamp, writeBatch, onSnapshot, disableNetwork, enableNetwork, runTransaction, Timestamp } from 'firebase/firestore';

let env;
const profile = (uid, role, extra = {}) => ({ uid, role, name: uid, email: `${uid}@example.test`, active: true, teamId: '', playerId: '', ...extra });
const db = uid => env.authenticatedContext(uid).firestore();
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-sportpro',
    firestore: { host: '127.0.0.1', port: 8188, rules: readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8') }
  });
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    const seed = {
      'users/henry': profile('henry', 'JUGADOR'),
      'users/admin': profile('admin', 'ADMIN'),
      'users/coach': profile('coach', 'ENTRENADOR'),
      'users/player': profile('player', 'JUGADOR', { teamId: 'a', playerId: 'one' }),
      'users/parent': profile('parent', 'PADRE', { teamId: 'a', playerId: 'one' }),
      'users/unassigned': profile('unassigned', 'JUGADOR'),
      'users/inactive': profile('inactive', 'ENTRENADOR', { active: false, isActive: true }),
      'users/legacy': { uid: 'legacy', role: 'ENTRENADOR', isActive: true },
      'teams/a': { active: true }, 'teams/b': { active: true },
      'teams/a/players/one': { active: true, userUid: 'player', parentUid: 'parent' },
      'teams/a/players/two': { active: true }, 'teams/b/players/one': { active: true },
      'teams/a/trainings/t': { active: true }, 'teams/b/trainings/t': { active: true },
      'teams/a/attendance/own': { playerId: 'one', trainingId: 't' },
      'teams/a/attendance/other': { playerId: 'two', trainingId: 't' },
      'matches/own': { teamA: 'a', teamB: 'c', active: true }, 'matches/other': { teamA: 'b', teamB: 'c', active: true },
      'matches/away': { teamA: 'c', teamB: 'a', active: true },
      'matches/legacy': { active: true },
      'matches/own/events/e': { type: 'GOAL' }, 'matches/other/events/e': { type: 'GOAL' },
      'matches/own/aiSummaries/published': { text: 'summary', state: 'APPROVED' }
    };
    for (const [path, data] of Object.entries(seed)) await setDoc(doc(context.firestore(), path), data);
  });
});
after(async () => { if (env) await env.cleanup(); });

test('registration permits exactly public roles and rejects ADMIN, linkage and foreign profile', async () => {
  for (const role of ['JUGADOR', 'PADRE']) {
    const uid = `new-${role}`;
    await assertSucceeds(setDoc(doc(db(uid), 'users', uid), profile(uid, role)));
  }
  for (const [uid, data] of [
    ['new-admin', profile('new-admin', 'ADMIN')],
    ['new-coach', profile('new-coach', 'ENTRENADOR')],
    ['linked', profile('linked', 'JUGADOR', { teamId: 'a', playerId: 'one' })],
    ['unknown', profile('unknown', 'OWNER')]
  ]) await assertFails(setDoc(doc(db(uid), 'users', uid), data));
  await assertFails(setDoc(doc(db('attacker'), 'users/foreign'), profile('foreign', 'JUGADOR')));
});

test('client profiles cannot promote roles, relink themselves or delete and re-register', async () => {
  for (const uid of ['player', 'parent', 'coach', 'admin']) {
    await assertFails(updateDoc(doc(db(uid), 'users', uid), { role: uid === 'admin' ? 'ENTRENADOR' : 'ADMIN' }));
    await assertFails(deleteDoc(doc(db(uid), 'users', uid)));
  }
  await assertFails(updateDoc(doc(db('player'), 'users/player'), { teamId: 'b' }));
  await assertFails(updateDoc(doc(db('parent'), 'users/parent'), { playerId: 'two' }));
  await assertFails(updateDoc(doc(db('player'), 'users/player'), { active: true, name: 'Changed' }));
  await assertSucceeds(updateDoc(doc(db('coach'), 'users/unassigned'), { teamId: 'a', playerId: 'one' }));
  await assertSucceeds(updateDoc(doc(db('coach'), 'users/unassigned'), { teamId: '', playerId: '' }));
});

test('players and parents see only assigned team, own ficha, trainings, attendance and matches', async () => {
  for (const uid of ['player', 'parent']) {
    const client = db(uid);
    for (const path of ['teams/a', 'teams/a/players/one', 'teams/a/trainings/t', 'teams/a/attendance/own', 'matches/own', 'matches/own/events/e', 'matches/own/aiSummaries/published']) await assertSucceeds(getDoc(doc(client, path)));
    for (const path of ['teams/b', 'teams/a/players/two', 'teams/b/players/one', 'teams/b/trainings/t', 'teams/a/attendance/other', 'matches/other', 'matches/legacy', 'matches/other/events/e', 'users/coach']) await assertFails(getDoc(doc(client, path)));
    await assertFails(getDocs(collection(client, 'teams')));
    await assertFails(getDocs(collection(client, 'teams/a/players')));
    await assertSucceeds(getDocs(query(collection(client, 'teams/a/attendance'), where('playerId', '==', 'one'))));
    await assertFails(getDocs(collection(client, 'teams/a/attendance')));
    await assertFails(getDocs(query(collection(client, 'teams/a/attendance'), where('playerId', '==', 'two'))));
    await assertSucceeds(getDocs(query(collection(client, 'matches'), where('teamA', '==', 'a'))));
    await assertSucceeds(getDocs(query(collection(client, 'matches'), where('teamB', '==', 'a'))));
    await assertFails(getDocs(collection(client, 'matches')));
    await assertFails(updateDoc(doc(client, 'teams/a/players/one'), { name: 'Self edit' }));
    await assertFails(setDoc(doc(client, 'teams/a/attendance/injected'), { playerId: 'one' }));
    await assertFails(setDoc(doc(client, 'matches/own/events/injected'), { type: 'GOAL' }));
  }
});

test('staff queries work without active filters; inactive and anonymous users fail closed', async () => {
  for (const uid of ['admin', 'coach', 'legacy']) {
    await assertSucceeds(getDocs(collection(db(uid), 'teams')));
    await assertSucceeds(getDocs(collection(db(uid), 'teams/a/players')));
    await assertSucceeds(setDoc(doc(db(uid), 'teams/a/trainings', uid), { active: true }));
  }
  await assertFails(getDocs(collection(db('inactive'), 'teams')));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'teams/a')));
  await assertFails(getDoc(doc(db('missing-profile'), 'teams/a')));
});


test('role approval matrix is enforced by Firestore with atomic writes', async () => {
  const actors = [
    ['coach', ['JUGADOR', 'PADRE']],
    ['admin', ['JUGADOR', 'PADRE', 'ENTRENADOR']],
    ['player', []], ['parent', []],
    ['henry', ['JUGADOR', 'PADRE', 'ADMIN']]
  ];
  const claims = uid => uid === 'henry' ? { email: 'harontovar@gmail.com', email_verified: true } : { email: `${uid}@example.test`, email_verified: true };
  for (const [actor, allowed] of actors) {
    const client = env.authenticatedContext(actor, claims(actor)).firestore();
    for (const toRole of ['JUGADOR', 'PADRE', 'ENTRENADOR', 'ADMIN']) {
      const uid = `req-${actor}-${toRole}`;
      const fromRole = toRole === 'JUGADOR' ? 'PADRE' : 'JUGADOR';
      await env.withSecurityRulesDisabled(async c => setDoc(doc(c.firestore(), 'users', uid), profile(uid, fromRole)));
      const owner = env.authenticatedContext(uid, { email: `${uid}@example.test` }).firestore();
      const request = { uid, email: `${uid}@example.test`, fromRole, toRole, status: 'PENDING', createdAt: serverTimestamp() };
      await assertSucceeds(setDoc(doc(owner, 'roleRequests', uid), request));
      await assertFails(updateDoc(doc(owner, 'users', uid), { role: toRole }));
      await assertFails(updateDoc(doc(client, 'users', uid), { role: toRole }));
      await assertFails(updateDoc(doc(client, 'roleRequests', uid), { status: 'APPROVED' }));
      const batch = writeBatch(client);
      batch.update(doc(client, 'users', uid), { role: toRole });
      batch.update(doc(client, 'roleRequests', uid), { status: 'APPROVED' });
      if (allowed.includes(toRole)) {
        await assertSucceeds(batch.commit());
        if ((await getDoc(doc(owner, 'users', uid))).data().role !== toRole) throw new Error('wrong approved role');
      } else await assertFails(batch.commit());
    }
    if (allowed.length) await assertSucceeds(getDocs(query(collection(client, 'roleRequests'), where('toRole', 'in', allowed))));
    if (actor !== 'henry') await assertFails(getDocs(collection(client, 'roleRequests')));
  }
});

test('role requests reject stale source roles, self approval, forged claims and preserve rejection', async () => {
  const owner = env.authenticatedContext('unassigned', { email: 'u@example.test' }).firestore();
  const creator = env.authenticatedContext('henry', { email: 'harontovar@gmail.com', email_verified: true }).firestore();
  const unverified = env.authenticatedContext('henry', { email: 'harontovar@gmail.com', email_verified: false }).firestore();
  const request = { uid: 'unassigned', email: 'u@example.test', fromRole: 'JUGADOR', toRole: 'ADMIN', status: 'PENDING', createdAt: serverTimestamp() };
  await assertFails(setDoc(doc(owner, 'roleRequests/player'), request));
  await assertFails(setDoc(doc(owner, 'roleRequests/unassigned'), { ...request, fromRole: 'ADMIN' }));
  await assertFails(setDoc(doc(owner, 'roleRequests/unassigned'), { ...request, email: 'forged@example.test' }));
  await assertSucceeds(setDoc(doc(owner, 'roleRequests/unassigned'), request));
  await assertFails(setDoc(doc(owner, 'roleRequests/unassigned'), { ...request, toRole: 'ENTRENADOR' }));
  await assertFails(getDocs(collection(unverified, 'roleRequests')));
  const spoof = writeBatch(unverified);
  spoof.update(doc(unverified, 'users/unassigned'), { role: 'ADMIN' });
  spoof.update(doc(unverified, 'roleRequests/unassigned'), { status: 'APPROVED' });
  await assertFails(spoof.commit());
  await assertSucceeds(updateDoc(doc(creator, 'roleRequests/unassigned'), { status: 'REJECTED' }));
  if ((await getDoc(doc(owner, 'users/unassigned'))).data().role !== 'JUGADOR') throw new Error('rejection changed role');
  await assertSucceeds(setDoc(doc(owner, 'roleRequests/unassigned'), request));
  await env.withSecurityRulesDisabled(async c => updateDoc(doc(c.firestore(), 'users/unassigned'), { role: 'PADRE' }));
  const stale = writeBatch(creator);
  stale.update(doc(creator, 'users/unassigned'), { role: 'ADMIN' });
  stale.update(doc(creator, 'roleRequests/unassigned'), { status: 'APPROVED' });
  await assertFails(stale.commit());
  await assertSucceeds(setDoc(doc(creator, 'roleRequests/henry'), { uid: 'henry', email: 'harontovar@gmail.com', fromRole: 'JUGADOR', toRole: 'ADMIN', status: 'PENDING', createdAt: serverTimestamp() }));
  const self = writeBatch(creator);
  self.update(doc(creator, 'users/henry'), { role: 'ADMIN' });
  self.update(doc(creator, 'roleRequests/henry'), { status: 'APPROVED' });
  await assertFails(self.commit());
});


async function newMatch(id) {
  const c = db('coach');
  await assertSucceeds(setDoc(doc(c, `matches/${id}`), { name:'Práctica',teamA:'a',teamB:'b',date:serverTimestamp(),status:'not_started',active:true,createdBy:'coach',eventVersion:0 }));
  await assertSucceeds(setDoc(doc(c, `matches/${id}/eventTypes/goal`), { label:'Gol',countsGoal:true,active:true,updatedBy:'coach',updatedAt:serverTimestamp() }));
  return c;
}
function eventData(extra={}) {
  return {typeId:'goal',typeLabel:'Gol',countsGoal:true,minute:null,teamId:'a',player:'',note:'Registro tardío',voided:false,
    revision:0,revisionId:'rev-0',createdBy:'coach',updatedBy:'coach',createdAt:serverTimestamp(),updatedAt:serverTimestamp(),...extra};
}
function eventBatch(c, matchId, id, previous, data, version) {
  const b=writeBatch(c);
  b.set(doc(c,`matches/${matchId}/events/${id}`),data);
  b.set(doc(c,`matches/${matchId}/events/${id}/revisions/${data.revisionId}`),{actorUid:'coach',at:serverTimestamp(),previousData:previous,eventData:data});
  b.update(doc(c,`matches/${matchId}`),{eventVersion:version,lastEventId:id});
  return b;
}
function waitSnapshot(ref, predicate) {
  let off;
  return new Promise((resolve,reject)=>{
    const timeout=setTimeout(()=>{off?.();reject(new Error('listener timeout'));},12000);
    off=onSnapshot(ref,s=>{if(predicate(s)){clearTimeout(timeout);off?.();resolve(s);}},e=>{clearTimeout(timeout);off?.();reject(e);});
  });
}

test('live events: atomic append, correction, void and immutable actor history; two clients see changes',async()=>{
  const c=await newMatch('live'); const reader=db('player');
  const ref=doc(reader,'matches/live/events/one');
  let seen=waitSnapshot(ref,s=>s.exists() && s.data().revision===0);
  await assertSucceeds(eventBatch(c,'live','one',{},eventData(),1).commit());
  assert.equal((await seen).data().minute,null);
  let old=(await getDoc(doc(c,'matches/live/events/one'))).data();
  let next={...old,minute:8,player:'Dorsal 9',revision:1,revisionId:'rev-1',updatedBy:'coach',updatedAt:serverTimestamp()};
  seen=waitSnapshot(ref,s=>s.exists() && s.data().revision===1);
  await assertSucceeds(eventBatch(c,'live','one',old,next,2).commit());
  assert.equal((await seen).data().minute,8);
  old=(await getDoc(doc(c,'matches/live/events/one'))).data();
  next={...old,voided:true,revision:2,revisionId:'rev-2',updatedAt:serverTimestamp()};
  seen=waitSnapshot(ref,s=>s.exists() && s.data().voided);
  await assertSucceeds(eventBatch(c,'live','one',old,next,3).commit()); await seen;
  assert.equal((await getDocs(collection(reader,'matches/live/events/one/revisions'))).size,3);
  await assertFails(deleteDoc(doc(c,'matches/live/events/one')));
  await assertFails(updateDoc(doc(c,'matches/live/events/one/revisions/rev-0'),{actorUid:'admin'}));
  await assertFails(updateDoc(doc(c,'matches/live/events/one'),{note:'sin historial'}));
  await assertFails(eventBatch(c,'live','forged',{},eventData({createdBy:'player'}),4).commit());
  await assertFails(eventBatch(db('player'),'live','reader',{},eventData(),4).commit());
  await assertFails(setDoc(doc(c,'matches/live/events/no-audit'),eventData()));
  await assertFails(eventBatch(c,'live','wrong-team',{},eventData({teamId:'foreign'}),4).commit());
  await assertFails(eventBatch(c,'live','minute',{},eventData({minute:181}),4).commit());
  const catalog=waitSnapshot(doc(reader,'matches/live/eventTypes/goal'),s=>s.exists() && s.data().label==='Anotación');
  await updateDoc(doc(c,'matches/live/eventTypes/goal'),{label:'Anotación',updatedAt:serverTimestamp(),updatedBy:'coach'}); await catalog;
  assert.equal((await getDoc(ref)).data().typeLabel,'Gol'); // historical snapshot is not rewritten
});

test('new events queue offline and synchronize when connection returns',async()=>{
  const c=await newMatch('offline');
  await getDoc(doc(c,'matches/offline'));
  await disableNetwork(c);
  const local=waitSnapshot(doc(c,'matches/offline/events/queued'),s=>s.exists() && s.metadata.hasPendingWrites);
  const committed=eventBatch(c,'offline','queued',{},eventData({minute:3}),1).commit();
  assert.equal((await local).metadata.hasPendingWrites,true);
  await enableNetwork(c); await assertSucceeds(committed);
  assert.equal((await getDoc(doc(db('parent'),'matches/offline/events/queued'))).data().minute,3);
});

test('summary draft is private, approval requires current event/status version',async()=>{
  const c=await newMatch('summary'); const base={text:'Sólo hechos',sourceHash:'a'.repeat(64),sourceVersion:0,state:'DRAFT',authorUid:'coach',approvedBy:'',updatedAt:serverTimestamp()};
  await assertSucceeds(setDoc(doc(c,'matches/summary/aiSummaries/draft'),base));
  await assertFails(getDoc(doc(db('player'),'matches/summary/aiSummaries/draft')));
  await assertFails(setDoc(doc(db('player'),'matches/summary/aiSummaries/published'),{...base,state:'APPROVED',approvedBy:'player',authorUid:'player'}));
  await assertSucceeds(setDoc(doc(c,'matches/summary/aiSummaries/published'),{...base,state:'APPROVED',approvedBy:'coach'}));
  await assertSucceeds(getDoc(doc(db('player'),'matches/summary/aiSummaries/published')));
  await updateDoc(doc(c,'matches/summary'),{status:'in_progress',eventVersion:1,updatedBy:'coach',updatedAt:serverTimestamp()});
  await assertFails(setDoc(doc(c,'matches/summary/aiSummaries/published'),{...base,state:'APPROVED',approvedBy:'coach'}));
  await assertSucceeds(setDoc(doc(c,'matches/summary/aiSummaries/published'),{...base,sourceVersion:1,state:'APPROVED',approvedBy:'coach'}));
});

const content=(uid,text)=>({text,authorUid:uid,state:'VISIBLE',createdAt:serverTimestamp(),updatedAt:serverTimestamp(),updatedBy:uid});
test('community: scoped team, comments, one reaction per user, private reports and staff moderation',async()=>{
  const p=db('player'),parent=db('parent'),c=db('coach'); const path='teams/a/posts/post';
  await assertSucceeds(setDoc(doc(p,path),content('player','Buen partido')));
  await assertFails(getDoc(doc(db('unassigned'),path)));
  await assertFails(setDoc(doc(p,'teams/b/posts/injected'),content('player','Ajeno')));
  await assertFails(setDoc(doc(p,'teams/a/posts/forged'),content('coach','Falso')));
  await assertSucceeds(setDoc(doc(parent,`${path}/comments/c`),content('parent','Ánimo')));
  await assertSucceeds(setDoc(doc(parent,`${path}/reactions/parent`),{kind:'LIKE',createdAt:serverTimestamp()}));
  await assertSucceeds(setDoc(doc(parent,`${path}/reactions/parent`),{kind:'LIKE',createdAt:serverTimestamp()}));
  assert.equal((await getDocs(collection(p,`${path}/reactions`))).size,1);
  await assertFails(setDoc(doc(p,`${path}/reactions/parent`),{kind:'LIKE',createdAt:serverTimestamp()}));
  await assertSucceeds(setDoc(doc(parent,`${path}/reports/parent`),{reason:'Revisar contenido',createdAt:serverTimestamp()}));
  await assertFails(getDoc(doc(p,`${path}/reports/parent`)));
  await assertSucceeds(getDoc(doc(c,`${path}/reports/parent`)));
  await assertFails(updateDoc(doc(p,path),{state:'HIDDEN',updatedBy:'player',updatedAt:serverTimestamp()}));
  await assertSucceeds(updateDoc(doc(c,path),{state:'HIDDEN',updatedBy:'coach',updatedAt:serverTimestamp()}));
  await assertFails(getDoc(doc(p,path)));
  await assertFails(getDoc(doc(parent,`${path}/comments/c`)));
  await assertFails(setDoc(doc(parent,`${path}/comments/after`),content('parent','Oculto')));
  await assertFails(deleteDoc(doc(c,path)));
  await assertSucceeds(getDoc(doc(c,path)));
});

// Family-link tests run only against demo-sportpro on localhost.
const familyGift=(teamId,playerId,name,expiresAt=Timestamp.fromMillis(Date.now()+1200000))=>({teamId,playerId,name,issuedBy:'coach',createdAt:serverTimestamp(),expiresAt,usedBy:'',usedAt:null});
async function redeemFamily(uid,code,extra={}) {
  const client=db(uid), user=doc(client,'users',uid), invitation=doc(client,'familyCodes',code);
  return runTransaction(client,async tx=>{
    const account=(await tx.get(user)).data(), gift=(await tx.get(invitation)).data();
    const children={...(account.children||{})};
    children[gift.teamId]={...(children[gift.teamId]||{}),[gift.playerId]:{name:gift.name,code},...extra};
    tx.update(user,{children,lastFamilyCode:code});
    tx.update(invitation,{usedBy:uid,usedAt:serverTimestamp()});
    tx.update(doc(client,'teams',gift.teamId,'players',gift.playerId),{parentUid:uid});
  });
}
test('family: several children in same and different teams, old links retained, strangers excluded',async()=>{
  await env.withSecurityRulesDisabled(async ctx=>{
    const rows={'users/family-a':profile('family-a','PADRE', {teamId:'a',playerId:'one'}),'users/family-b':profile('family-b','PADRE'),
      'users/family-off':profile('family-off','PADRE',{active:false}),
      'teams/fx':{active:true},'teams/fy':{active:true},
      'teams/fx/players/k1':{name:'Uno',active:true,parentUid:null},'teams/fx/players/k2':{name:'Dos',active:true,parentUid:null},
      'teams/fy/players/k3':{name:'Tres',active:true,parentUid:null},'teams/fx/attendance/k1':{playerId:'k1'},
      'teams/fx/attendance/other':{playerId:'stranger'},'teams/fy/trainings/t':{active:true},
      'matches/family':{teamA:'fy',teamB:'outside',active:true}};
    for(const [p,d]of Object.entries(rows))await setDoc(doc(ctx.firestore(),p),d);
  });
  for(const [code,t,p,n]of [['ABCDEFGH2345','fx','k1','Uno'],['ABCDEFGH2346','fx','k2','Dos'],['ABCDEFGH2347','fy','k3','Tres']]) {
    await assertSucceeds(setDoc(doc(db('coach'),'familyCodes',code),familyGift(t,p,n)));
    await assertSucceeds(redeemFamily('family-a',code));
  }
  for(const path of ['teams/fx','teams/fy','teams/fx/players/k1','teams/fx/players/k2','teams/fy/players/k3','teams/fy/trainings/t','matches/family','teams/a/players/one'])
    await assertSucceeds(getDoc(doc(db('family-a'),path)));
  await assertSucceeds(getDocs(query(collection(db('family-a'),'teams/fx/attendance'),where('playerId','==','k1'))));
  for(const path of ['teams/fx/players/k1','teams/fx/attendance/k1','matches/family']) await assertFails(getDoc(doc(db('family-b'),path)));
  await assertFails(getDoc(doc(db('family-a'),'teams/fx/attendance/other')));
  await assertFails(getDocs(collection(db('family-a'),'teams/fx/players')));
  await assertFails(getDocs(collection(db('family-a'),'familyCodes')));
  await assertFails(redeemFamily('family-b','ABCDEFGH2345'));await assertFails(redeemFamily('family-a','ABCDEFGH2345'));
  const account=(await getDoc(doc(db('family-a'),'users/family-a'))).data();
  assert.equal(Object.keys(account.children.fx).length,2);assert.equal(account.teamId,'a');assert.equal(account.playerId,'one');
});
test('family: expiry, wrong role, inactive, anonymous, forged profile and non-atomic writes fail',async()=>{
  const code='ABCDEFGH2355';await assertSucceeds(setDoc(doc(db('coach'),'familyCodes',code),familyGift('fx','k1','Uno')));
  for(const uid of ['family-b','player','family-off']) {
    await assertFails(updateDoc(doc(db(uid),'users',uid),{children:{fx:{k1:{name:'Uno',code}}},lastFamilyCode:code}));
    await assertFails(setDoc(doc(db(uid),'familyCodes','ABCDEFGH2366'),familyGift('fx','k1','Uno')));
  }
  for(const uid of ['player','family-off']) await assertFails(redeemFamily(uid,code));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),'familyCodes',code)));
  await assertFails(updateDoc(doc(db('family-b'),'familyCodes',code),{usedBy:'family-b',usedAt:serverTimestamp()}));
  await assertFails(updateDoc(doc(db('family-b'),'teams/fx/players/k1'),{parentUid:'family-b'}));
  await assertFails(redeemFamily('family-b',code)); // fresh code cannot steal an already-owned child
  await assertFails(setDoc(doc(db('coach'),'familyCodes','BAD'),familyGift('fx','k1','Uno')));
  for(const expiresAt of [Timestamp.fromMillis(Date.now()-1000),Timestamp.fromMillis(Date.now()+3600000)])
    await assertFails(setDoc(doc(db('coach'),'familyCodes','ABCDEFGH2366'),familyGift('fx','k1','Uno',expiresAt)));
  await env.withSecurityRulesDisabled(async ctx=>setDoc(doc(ctx.firestore(),'familyCodes/ABCDEFGH2367'),familyGift('fx','k1','Uno',Timestamp.fromMillis(Date.now()-1000))));
  await assertFails(redeemFamily('family-b','ABCDEFGH2367'));
  await assertFails(setDoc(doc(db('injected'),'users/injected'),profile('injected','PADRE',{children:{fx:{k1:{name:'Uno',code}}}})));
});
test('family: race has one winner and valid code cannot smuggle other children or mutate role',async()=>{
  await env.withSecurityRulesDisabled(async ctx=>{
    for(const [p,d]of Object.entries({'teams/fy/players/race':{name:'Carrera',active:true,parentUid:null},
      'users/racer-a':profile('racer-a','PADRE'),'users/racer-b':profile('racer-b','PADRE')}))await setDoc(doc(ctx.firestore(),p),d);
  });
  for(const code of ['ABCDEFGH2377','ABCDEFGH2378'])await assertSucceeds(setDoc(doc(db('coach'),'familyCodes',code),familyGift('fy','race','Carrera')));
  await assertFails(redeemFamily('racer-a','ABCDEFGH2377',{stranger:{name:'Otra',code:'ABCDEFGH2377'}}));
  const results=await Promise.allSettled([redeemFamily('racer-a','ABCDEFGH2377'),redeemFamily('racer-b','ABCDEFGH2378')]);
  assert.equal(results.filter(x=>x.status==='fulfilled').length,1);
  const winner=results[0].status==='fulfilled'?'racer-a':'racer-b',loser=winner==='racer-a'?'racer-b':'racer-a';
  await assertSucceeds(getDoc(doc(db(winner),'teams/fy/players/race')));
  await assertFails(getDoc(doc(db(loser),'teams/fy/players/race')));
});
test('family: all three writes are mandatory; extra fields, extra teams and role changes cannot ride a code',async()=>{
  const code='ABCDEFGH2388', client=db('family-b');
  await env.withSecurityRulesDisabled(async ctx=>setDoc(doc(ctx.firestore(),'teams/fx/players/atomic'),{name:'Atómico',active:true,parentUid:null}));
  await assertSucceeds(setDoc(doc(db('coach'),'familyCodes',code),familyGift('fx','atomic','Atómico')));
  for(const omitted of ['code','profile','child']) {
    const batch=writeBatch(client);
    if(omitted!=='profile') batch.update(doc(client,'users/family-b'),{children:{fx:{atomic:{name:'Atómico',code}}},lastFamilyCode:code});
    if(omitted!=='code') batch.update(doc(client,'familyCodes',code),{usedBy:'family-b',usedAt:serverTimestamp()});
    if(omitted!=='child') batch.update(doc(client,'teams/fx/players/atomic'),{parentUid:'family-b'});
    await assertFails(batch.commit());
  }
  for(const injection of [
    {role:'ADMIN'}, {children:{fx:{atomic:{name:'Atómico',code}},fy:{k3:{name:'Tres',code}}}},
    {children:{fx:{atomic:{name:'Fake',code}}}}
  ]) {
    const batch=writeBatch(client);
    batch.update(doc(client,'users/family-b'),{children:{fx:{atomic:{name:'Atómico',code}}},lastFamilyCode:code,...injection});
    batch.update(doc(client,'familyCodes',code),{usedBy:'family-b',usedAt:serverTimestamp()});
    batch.update(doc(client,'teams/fx/players/atomic'),{parentUid:'family-b'});
    await assertFails(batch.commit());
  }
  await assertSucceeds(redeemFamily('family-b',code));
  const staff=(await getDoc(doc(db('coach'),'familyCodes',code))).data();assert.equal(staff.usedBy,'family-b');
});
test('family: community and read-only match access extend only to linked teams, inactive child never grants access',async()=>{
  await assertSucceeds(setDoc(doc(db('family-a'),'teams/fy/posts/family'),content('family-a','Nuestro equipo')));
  await assertSucceeds(getDoc(doc(db('family-a'),'teams/fy/posts/family')));
  // family-b only owns a child in fx after the atomic test, not fy.
  await assertFails(getDoc(doc(db('family-b'),'teams/fy/posts/family')));
  await assertFails(setDoc(doc(db('family-b'),'teams/fy/posts/foreign'),content('family-b','No es mi equipo')));
  await assertFails(updateDoc(doc(db('family-a'),'matches/family'),{status:'finished'}));
  await env.withSecurityRulesDisabled(async ctx=>setDoc(doc(ctx.firestore(),'teams/fx/players/inactive'),{name:'Inactivo',active:false,parentUid:null}));
  await assertSucceeds(setDoc(doc(db('coach'),'familyCodes/ABCDEFGH2399'),familyGift('fx','inactive','Inactivo')));
  await assertFails(redeemFamily('family-b','ABCDEFGH2399'));
});


test('coach signup: direct staff creation is denied; requesting does not grant family access; ADMIN approval does',async()=>{
  const uid='coach-applicant', client=env.authenticatedContext(uid,{email:`${uid}@example.test`}).firestore();
  await assertFails(setDoc(doc(client,'users',uid),profile(uid,'ENTRENADOR')));
  await assertSucceeds(setDoc(doc(client,'users',uid),profile(uid,'JUGADOR')));
  await assertSucceeds(setDoc(doc(client,'roleRequests',uid),{uid,email:`${uid}@example.test`,fromRole:'JUGADOR',toRole:'ENTRENADOR',status:'PENDING',createdAt:serverTimestamp()}));
  await assertFails(getDocs(collection(client,'teams')));
  await assertFails(getDoc(doc(client,'teams/fx/players/k1')));
  await assertFails(getDoc(doc(client,'users/family-a')));
  const approve=(actor)=>{const c=db(actor),b=writeBatch(c);b.update(doc(c,'users',uid),{role:'ENTRENADOR'});b.update(doc(c,'roleRequests',uid),{status:'APPROVED'});return b.commit()};
  await assertFails(approve('coach'));
  await assertFails(approve('inactive'));
  await assertFails(updateDoc(doc(db('admin'),'users',uid),{role:'ENTRENADOR'}));
  await assertFails(updateDoc(doc(db('admin'),'roleRequests',uid),{status:'APPROVED'}));
  assert.equal((await getDoc(doc(client,'users',uid))).data().role,'JUGADOR');
  await assertSucceeds(approve('admin'));
  assert.equal((await getDoc(doc(client,'users',uid))).data().role,'ENTRENADOR');
  await assertSucceeds(getDocs(collection(client,'teams')));
  await assertSucceeds(getDoc(doc(client,'teams/fx/players/k1')));
  await assertSucceeds(getDoc(doc(client,'users/family-a')));
});
test('coach signup: creator without ADMIN cannot approve coach, existing coaches retain staff and nonstaff approval',async()=>{
  const uid='creator-coach-applicant', owner=env.authenticatedContext(uid,{email:`${uid}@example.test`}).firestore();
  await assertSucceeds(setDoc(doc(owner,'users',uid),profile(uid,'PADRE')));
  await assertSucceeds(setDoc(doc(owner,'roleRequests',uid),{uid,email:`${uid}@example.test`,fromRole:'PADRE',toRole:'ENTRENADOR',status:'PENDING',createdAt:serverTimestamp()}));
  const creator=env.authenticatedContext('henry',{email:'harontovar@gmail.com',email_verified:true}).firestore();
  const batch=writeBatch(creator);batch.update(doc(creator,'users',uid),{role:'ENTRENADOR'});batch.update(doc(creator,'roleRequests',uid),{status:'APPROVED'});
  await assertFails(batch.commit());
  // Existing role is not rewritten or revoked by the new self-registration restriction.
  for(const id of ['coach','legacy']) {await assertSucceeds(getDocs(collection(db(id),'teams')));await assertSucceeds(getDoc(doc(db(id),'teams/fx/players/k1')))}
});

test('family role transition: clear children atomically and never reactivate on return',async()=>{
  const uid='role-family', email=uid+'@example.test',owner=env.authenticatedContext(uid,{email}).firestore();
  await env.withSecurityRulesDisabled(async c=>setDoc(doc(c.firestore(),'users',uid),profile(uid,'PADRE',{children:{fx:{k1:{name:'Uno',code:'ABCDEFGH2345'}}}})));
  const request=(fromRole,toRole)=>setDoc(doc(owner,'roleRequests',uid),{uid,email,fromRole,toRole,status:'PENDING',createdAt:serverTimestamp()});
  await assertSucceeds(request('PADRE','JUGADOR'));
  const coach=db('coach');const bad=writeBatch(coach);bad.update(doc(coach,'users',uid),{role:'JUGADOR'});bad.update(doc(coach,'roleRequests',uid),{status:'APPROVED'});await assertFails(bad.commit());
  const good=writeBatch(coach);good.update(doc(coach,'users',uid),{role:'JUGADOR',children:{}});good.update(doc(coach,'roleRequests',uid),{status:'APPROVED'});await assertSucceeds(good.commit());
  assert.deepEqual((await getDoc(doc(owner,'users',uid))).data().children,{});
  await assertFails(getDoc(doc(owner,'teams/fx/players/k1')));
  await assertSucceeds(request('JUGADOR','PADRE'));
  const back=writeBatch(coach);back.update(doc(coach,'users',uid),{role:'PADRE'});back.update(doc(coach,'roleRequests',uid),{status:'APPROVED'});await assertSucceeds(back.commit());
  await assertFails(getDoc(doc(owner,'teams/fx/players/k1')));
  assert.deepEqual((await getDoc(doc(owner,'users',uid))).data().children,{});
});
test('family role transition: old maps on a non-parent also cannot reactivate via a role approval',async()=>{
  const uid='old-map',email=uid+'@example.test',owner=env.authenticatedContext(uid,{email}).firestore(),coach=db('coach');
  await env.withSecurityRulesDisabled(async c=>setDoc(doc(c.firestore(),'users',uid),profile(uid,'JUGADOR',{children:{fx:{k1:{name:'Old',code:'ABCDEFGH2345'}}}})));
  await assertSucceeds(setDoc(doc(owner,'roleRequests',uid),{uid,email,fromRole:'JUGADOR',toRole:'PADRE',status:'PENDING',createdAt:serverTimestamp()}));
  const bad=writeBatch(coach);bad.update(doc(coach,'users',uid),{role:'PADRE'});bad.update(doc(coach,'roleRequests',uid),{status:'APPROVED'});await assertFails(bad.commit());
  const good=writeBatch(coach);good.update(doc(coach,'users',uid),{role:'PADRE',children:{}});good.update(doc(coach,'roleRequests',uid),{status:'APPROVED'});await assertSucceeds(good.commit());
  await assertFails(getDoc(doc(owner,'teams/fx/players/k1')));
});

// Same three-document reader, now with ADMIN-only two-document revocation.
async function revokeFamily(actor,uid,team,player,options={}) {
  const c=db(actor), user=doc(c,'users',uid), child=doc(c,'teams',team,'players',player);
  return runTransaction(c,async tx=>{
    const a=(await tx.get(user)).data(),p=(await tx.get(child)).data();
    const children={...(a.children||{})}, group={...(children[team]||{})};
    delete group[player]; if(Object.keys(group).length)children[team]=group;else delete children[team];
    const fields={children,lastFamilyRevocation:{teamId:team,playerId:player,by:actor,at:serverTimestamp()},...(options.extra||{})};
    if(a.teamId===team && a.playerId===player){fields.teamId='';fields.playerId=''}
    if(!options.skipProfile)tx.update(user,fields);
    if(!options.skipChild)tx.update(child,{parentUid:p.parentUid===uid?null:(p.parentUid??null),familyRevokedAt:serverTimestamp(),familyRevokedParentUid:uid,...(options.extraChild||{})});
  });
}
test('ADMIN revokes one child atomically: denies subsequent child and attendance reads, preserves siblings and other teams',async()=>{
  await env.withSecurityRulesDisabled(async c=>{
    for(const [p,d]of Object.entries({
      'users/revoke-parent':profile('revoke-parent','PADRE',{teamId:'rx',playerId:'a'}),
      'teams/rx':{active:true},'teams/ry':{active:true},
      'teams/rx/players/a':{name:'A',parentUid:null,active:true},'teams/rx/players/b':{name:'B',parentUid:null,active:true},
      'teams/ry/players/c':{name:'C',parentUid:null,active:true},'teams/rx/attendance/a':{playerId:'a'}
    }))await setDoc(doc(c.firestore(),p),d);
  });
  for(const [code,t,p,n]of [['ABCDEFGH2455','rx','a','A'],['ABCDEFGH2456','rx','b','B'],['ABCDEFGH2457','ry','c','C']]){
    await assertSucceeds(setDoc(doc(db('coach'),'familyCodes',code),familyGift(t,p,n)));await assertSucceeds(redeemFamily('revoke-parent',code));
  }
  await assertSucceeds(setDoc(doc(db('coach'),'familyCodes/ABCDEFGH2458'),familyGift('rx','a','A')));
  await assertSucceeds(getDoc(doc(db('revoke-parent'),'teams/rx/players/a')));
  await assertSucceeds(getDoc(doc(db('revoke-parent'),'teams/rx/attendance/a')));
  await assertFails(revokeFamily('coach','revoke-parent','rx','a'));
  await assertFails(revokeFamily('family-b','revoke-parent','rx','a'));
  await assertFails(updateDoc(doc(db('coach'),'teams/rx/players/a'),{parentUid:null}));
  await assertFails(updateDoc(doc(db('coach'),'users/revoke-parent'),{teamId:'',playerId:''}));
  for(const options of [{skipChild:true},{skipProfile:true},{extra:{role:'JUGADOR'}},{extra:{children:{}}}])
    await assertFails(revokeFamily('admin','revoke-parent','rx','a',options));
  await assertSucceeds(revokeFamily('admin','revoke-parent','rx','a'));
  await assertFails(getDoc(doc(db('revoke-parent'),'teams/rx/players/a')));
  await assertFails(getDoc(doc(db('revoke-parent'),'teams/rx/attendance/a')));
  for(const path of ['teams/rx/players/b','teams/ry/players/c'])await assertSucceeds(getDoc(doc(db('revoke-parent'),path)));
  const user=(await getDoc(doc(db('revoke-parent'),'users/revoke-parent'))).data();
  assert.equal(user.teamId,'');assert.equal(user.playerId,'');assert.equal(user.children.rx.a,undefined);
  assert.equal((await getDoc(doc(db('admin'),'teams/rx/players/a'))).data().parentUid,null);
  await assertFails(redeemFamily('revoke-parent','ABCDEFGH2458')); // old, unused bearer code is invalidated
  await assertFails(updateDoc(doc(db('coach'),'teams/rx/players/a'),{familyRevokedAt:null}));
  await assertSucceeds(setDoc(doc(db('coach'),'familyCodes/ABCDEFGH2459'),familyGift('rx','a','A')));
  await assertSucceeds(redeemFamily('revoke-parent','ABCDEFGH2459'));
  await assertFails(revokeFamily('revoke-parent','revoke-parent','rx','a'));
});
test('ADMIN revokes legacy-only and last-child links; stale map removal preserves the actual other parent',async()=>{
  await env.withSecurityRulesDisabled(async c=>{
    for(const [p,d]of Object.entries({
      'users/legacy-revoke':profile('legacy-revoke','PADRE',{teamId:'rx',playerId:'legacy'}),
      'teams/rx/players/legacy':{name:'Legacy',parentUid:'legacy-revoke',active:true},
      'users/stale-parent':profile('stale-parent','PADRE',{children:{rx:{b:{name:'B',code:'old'}}}}),
      'users/inactive-admin':profile('inactive-admin','ADMIN',{active:false})
    }))await setDoc(doc(c.firestore(),p),d);
  });
  await assertSucceeds(getDoc(doc(db('legacy-revoke'),'teams/rx/players/legacy')));
  await assertFails(revokeFamily('inactive-admin','legacy-revoke','rx','legacy'));
  await assertSucceeds(revokeFamily('admin','legacy-revoke','rx','legacy'));
  await assertFails(getDoc(doc(db('legacy-revoke'),'teams/rx/players/legacy')));
  await assertSucceeds(revokeFamily('admin','stale-parent','rx','b'));
  await assertFails(getDoc(doc(db('stale-parent'),'teams/rx/players/b')));
  await assertSucceeds(getDoc(doc(db('revoke-parent'),'teams/rx/players/b')));
  assert.equal((await getDoc(doc(db('admin'),'teams/rx/players/b'))).data().parentUid,'revoke-parent');
  assert.deepEqual((await getDoc(doc(db('stale-parent'),'users/stale-parent'))).data().children,{});
});
test('revocation hardening: stale grant without pointer, wrong actor stamp, extra child mutation and non-ADMIN fail closed',async()=>{
  await env.withSecurityRulesDisabled(async c=>{
    await setDoc(doc(c.firestore(),'users/stale-empty'),profile('stale-empty','PADRE',{children:{rx:{empty:{name:'Old',code:'old'}}}}));
    await setDoc(doc(c.firestore(),'teams/rx/players/empty'),{name:'Old',parentUid:null,active:true});
  });
  await assertSucceeds(getDoc(doc(db('stale-empty'),'teams/rx/players/empty')));
  await assertFails(revokeFamily('stale-empty','stale-empty','rx','empty'));
  await assertFails(revokeFamily('admin','stale-empty','rx','empty',{extra:{lastFamilyRevocation:{teamId:'rx',playerId:'empty',by:'coach',at:serverTimestamp()}}}));
  await assertFails(revokeFamily('admin','stale-empty','rx','empty',{extraChild:{name:'Injected'}}));
  await assertSucceeds(revokeFamily('admin','stale-empty','rx','empty'));
  await assertFails(getDoc(doc(db('stale-empty'),'teams/rx/players/empty')));
  await assertFails(updateDoc(doc(db('coach'),'teams/rx/players/empty'),{familyRevokedParentUid:'someone'}));
  await assertSucceeds(updateDoc(doc(db('coach'),'teams/rx/players/empty'),{name:'Legitimate edit'}));
});
