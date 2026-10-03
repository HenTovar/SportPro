import { after, before, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, doc, getDoc, getDocs, query, setDoc, updateDoc, deleteDoc, where } from 'firebase/firestore';

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
      'matches/own/aiSummaries/s': { text: 'summary' }
    };
    for (const [path, data] of Object.entries(seed)) await setDoc(doc(context.firestore(), path), data);
  });
});
after(async () => { if (env) await env.cleanup(); });

test('registration permits exactly public roles and rejects ADMIN, linkage and foreign profile', async () => {
  for (const role of ['ENTRENADOR', 'JUGADOR', 'PADRE']) {
    const uid = `new-${role}`;
    await assertSucceeds(setDoc(doc(db(uid), 'users', uid), profile(uid, role)));
  }
  for (const [uid, data] of [
    ['new-admin', profile('new-admin', 'ADMIN')],
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
    for (const path of ['teams/a', 'teams/a/players/one', 'teams/a/trainings/t', 'teams/a/attendance/own', 'matches/own', 'matches/own/events/e', 'matches/own/aiSummaries/s']) await assertSucceeds(getDoc(doc(client, path)));
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
