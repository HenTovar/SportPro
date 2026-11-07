import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {initializeTestEnvironment,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDocs,query,collection,where,getDocFromCache,disableNetwork,enableNetwork,onSnapshot} from 'firebase/firestore';

test('feature listeners use existing queries and own attendance scope; offline read stays cached',async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-sportpro',firestore:{host:'127.0.0.1',port:8188,rules:readFileSync(new URL('../../firestore.rules',import.meta.url),'utf8')}});
 try {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async c=>{
   for(const[path,value]of Object.entries({
    'users/coach':{uid:'coach',role:'ENTRENADOR',active:true},
    'users/parent':{uid:'parent',role:'PADRE',active:true,teamId:'a',playerId:'one'},
    'teams/a':{active:true},'teams/b':{active:true},
    'teams/a/trainings/t':{name:'Session',date:1794000000000,status:'PROGRAMADO'},
    'teams/a/attendance/one':{trainingId:'t',playerId:'one',status:'ASISTIO'},
    'teams/a/attendance/two':{trainingId:'t',playerId:'two',status:'FALTA'},
    'matches/home':{teamA:'a',teamB:'b',status:'finished',date:1794000000000,active:true},
    'matches/away':{teamA:'b',teamB:'a',status:'not_started',date:1795000000000,active:true},
    'matches/home/events/g':{teamId:'a',player:'Fictional Player',countsGoal:true,voided:false}
   }))await setDoc(doc(c.firestore(),path),value)
  });
  const parent=env.authenticatedContext('parent').firestore(),coach=env.authenticatedContext('coach').firestore();
  for(const client of [parent,coach]) {
   assert.equal((await getDocs(query(collection(client,'matches'),where('teamA','==','a')))).size,1);
   assert.equal((await getDocs(query(collection(client,'matches'),where('teamB','==','a')))).size,1);
   assert.equal((await getDocs(collection(client,'teams/a/trainings'))).size,1);
   assert.equal((await getDocs(collection(client,'matches/home/events'))).size,1);
  }
  assert.equal((await getDocs(collection(coach,'teams/a/attendance'))).size,2);
  await assertFails(getDocs(collection(parent,'teams/a/attendance')));
  assert.equal((await getDocs(query(collection(parent,'teams/a/attendance'),where('playerId','==','one')))).size,1);
  await assertFails(getDocs(query(collection(parent,'teams/a/attendance'),where('playerId','==','two'))));
  await disableNetwork(parent);
  assert.equal((await getDocFromCache(doc(parent,'teams/a/trainings/t'))).data().name,'Session');
  assert.equal((await getDocFromCache(doc(parent,'teams/a/trainings/t'))).metadata.fromCache,true);
  await enableNetwork(parent);
 }finally{await env.cleanup()}
});
