// Identical attacks run against code8 and the successor. No candidate-only helper is used.
import {test,before,after,beforeEach} from 'node:test';
import {readFileSync} from 'node:fs';
import {initializeTestEnvironment,assertFails} from '@firebase/rules-unit-testing';
import {doc,getDocFromServer,setDoc,deleteDoc,serverTimestamp,Timestamp} from 'firebase/firestore';
let env;const db=uid=>env.authenticatedContext(uid,{email:uid+'@example.test',email_verified:true}).firestore();
const profile=(uid,role,extra={})=>({uid,role,active:true,teamId:'',playerId:'',...extra});
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-sportpro-security',firestore:{host:'127.0.0.1',port:8193,rules:readFileSync(process.env.SPORTPRO_RULES_FILE||new URL('../../firestore.rules',import.meta.url),'utf8')}})});
after(async()=>{await env.cleanup()});
beforeEach(async()=>{await env.clearFirestore();await env.withSecurityRulesDisabled(async c=>{
 const d=c.firestore();for(const[p,row]of Object.entries({
 'users/admin':profile('admin','ADMIN'),'users/coach':profile('coach','ENTRENADOR',{coachTeamIds:['a']}),
 'users/parent':profile('parent','JUGADOR',{teamId:'b',playerId:'one',lastFamilyRevocation:{teamId:'b',playerId:'one',by:'admin',at:Timestamp.now()}}),
 'teams/a':{ownerId:'admin'},'teams/b':{ownerId:'admin'},
 'teams/a/players/one':{active:true,name:'Own',parentUid:null},
 'teams/b/players/one':{active:true,name:'Foreign',parentUid:null,familyRevokedParentUid:'parent',familyRevokedAt:Timestamp.now()}
 }))await setDoc(doc(d,p),row);
})});
test('C1 coach must not read an unassigned family in foreign team',async()=>{
 await assertFails(getDocFromServer(doc(db('coach'),'teams/b/players/one')));
});
test('C2 ADMIN must not delete sealed child and revive pre-revocation invitations',async()=>{
 await assertFails(deleteDoc(doc(db('admin'),'teams/b/players/one')));
});
test('C3 historical role-cycle and legacy pair must not reactivate revoked parent',async()=>{
 await assertFails(getDocFromServer(doc(db('parent'),'teams/b/players/one')));
});
test('C4 code issuance without quota and retaining minor name must be rejected',async()=>{
 await assertFails(setDoc(doc(db('coach'),'familyCodes/ABCDEFGH2345'),{teamId:'a',playerId:'one',name:'Own',issuedBy:'coach',createdAt:serverTimestamp(),expiresAt:Timestamp.fromMillis(Date.now()+1200000),usedBy:'',usedAt:null}));
});
