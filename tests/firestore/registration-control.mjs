// This contract must reject the old rules. All data is a local demo fixture.
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {test} from 'node:test';
import {initializeTestEnvironment} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDoc} from 'firebase/firestore';
test('unapproved signup cannot become staff or read another family',async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-sportpro',firestore:{host:'127.0.0.1',port:8188,rules:readFileSync(process.argv[2]||new URL('../../firestore.rules',import.meta.url),'utf8')}});
 try {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async c=>{
   await setDoc(doc(c.firestore(),'users/other-family'),{uid:'other-family',role:'PADRE',active:true,teamId:'private',playerId:'child'});
   await setDoc(doc(c.firestore(),'teams/private/players/child'),{name:'Private fixture',active:true,parentUid:'other-family'});
  });
  const attacker=env.authenticatedContext('unapproved',{email:'unapproved@example.test'}).firestore();
  let registered=false, familyRead=false;
  try {await setDoc(doc(attacker,'users/unapproved'),{uid:'unapproved',name:'Applicant',email:'unapproved@example.test',role:'ENTRENADOR',active:true,teamId:'',playerId:'',createdAt:0});registered=true}
  catch(e){if(e.code!=='permission-denied')throw e}
  try {familyRead=(await getDoc(doc(attacker,'teams/private/players/child'))).exists()}
  catch(e){if(e.code!=='permission-denied')throw e}
  console.log(JSON.stringify({unsafe_self_registration:registered,unauthorized_family_read:familyRead}));
  assert.equal(registered,false,'unapproved user became staff');assert.equal(familyRead,false,'unapproved user read another family');
 }finally{await env.cleanup()}
});
