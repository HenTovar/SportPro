import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {test} from 'node:test';
import {initializeTestEnvironment,assertFails,assertSucceeds} from '@firebase/rules-unit-testing';
import {doc,getDocFromServer,setDoc,writeBatch,serverTimestamp} from 'firebase/firestore';

test('an ADMIN can revoke one complete link and the parent loses server access',async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-sportpro',firestore:{host:'127.0.0.1',port:8188,
   rules:readFileSync(process.argv[2]||new URL('../../firestore.rules',import.meta.url),'utf8')}});
 try {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async c=>{
   await setDoc(doc(c.firestore(),'users/admin'),{uid:'admin',role:'ADMIN',active:true});
   await setDoc(doc(c.firestore(),'users/parent'),{uid:'parent',role:'PADRE',active:true,teamId:'t',playerId:'k',children:{t:{k:{name:'Fixture',code:'used'}}}});
   await setDoc(doc(c.firestore(),'teams/t/players/k'),{name:'Fixture',parentUid:'parent',active:true});
   await setDoc(doc(c.firestore(),'teams/t/attendance/k'),{playerId:'k'});
  });
  const parent=env.authenticatedContext('parent').firestore(),admin=env.authenticatedContext('admin').firestore();
  await assertSucceeds(getDocFromServer(doc(parent,'teams/t/players/k')));
  const b=writeBatch(admin);
  b.update(doc(admin,'users/parent'),{children:{},teamId:'',playerId:'',lastFamilyRevocation:{teamId:'t',playerId:'k',by:'admin',at:serverTimestamp()}});
  b.update(doc(admin,'teams/t/players/k'),{parentUid:null,familyRevokedAt:serverTimestamp(),familyRevokedParentUid:'parent'});
  await assertSucceeds(b.commit());
  await assertFails(getDocFromServer(doc(parent,'teams/t/players/k')));
  await assertFails(getDocFromServer(doc(parent,'teams/t/attendance/k')));
  assert.equal((await getDocFromServer(doc(admin,'teams/t/players/k'))).data().parentUid,null);
  console.log('ADMIN_REVOKE_CONFIRMED parent_child_read=DENIED parent_attendance_read=DENIED legacy_cleared=true');
 }finally{await env.cleanup()}
});
