package pe.edu.esan.sportpro.features

import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*
import java.util.TimeZone

class TeamInsightsTest {
    private val utc=TimeZone.getTimeZone("UTC")
    private fun date(y:Int,m:Int,d:Int)=Month(y,m).calendar(utc).apply { set(java.util.Calendar.DAY_OF_MONTH,d) }.timeInMillis
    private fun goal(id:String,player:String="Alex",team:String="a",voided:Boolean=false)=MatchEvent(id=id,player=player,teamId=team,countsGoal=true,voided=voided)
    @Test fun leapFebruaryAndMondayOffset() {assertEquals(29,Month(2024,2).days(utc));assertEquals(28,Month(2025,2).days(utc));assertEquals(0,Month(2026,6).leading(utc))}
    @Test fun monthBoundariesAndUnknownDate() {val m=Month(2026,11);assertTrue(m.contains(date(2026,11,1),utc));assertTrue(m.contains(date(2026,11,30),utc));assertFalse(m.contains(date(2026,12,1),utc));assertFalse(m.contains(0,utc))}
    @Test fun shiftAcrossYear() {assertEquals(Month(2027,1),Month(2026,12).shift(1));assertEquals(Month(2025,12),Month(2026,1).shift(-1))}
    @Test fun invalidMonthRejected() {assertThrows(IllegalArgumentException::class.java){Month(2026,13)}}
    @Test fun goalsExcludeVoidedRivalsAndNonGoals() {val rows=listOf(goal("1"),goal("2",voided=true),goal("3",team="b"),goal("4").copy(countsGoal=false));assertEquals(listOf("Alex" to 1),TeamInsights.goals("a",mapOf("m" to rows)))}
    @Test fun goalsGroupByRecordedNameAndUnknownBucket() {val result=TeamInsights.goals("a",mapOf("m" to listOf(goal("1"," Alex "),goal("2","alex"),goal("3",""))));assertEquals(2,result.first().second);assertTrue(result.contains("Sin identificar" to 1))}
    @Test fun correctionsReplaceRatherThanAccumulate() {assertEquals(0,TeamInsights.goals("a",mapOf("m" to listOf(goal("1").copy(voided=true,revision=1)))).size)}
    @Test fun attendanceUsesTrainingDateNotWriteDate() {val session=Training(id="t",date=date(2026,10,31));val row=AttendanceRecord(playerId="p",trainingId="t",recordedAt=date(2026,11,1));assertEquals(0,TeamInsights.attendance(Month(2026,11),listOf(row),listOf(session),utc).total)}
    @Test fun attendanceLatestRecordWinsForEachPlayerAndTraining() {val ts=listOf(Training(id="t",date=date(2026,11,1)));val a=AttendanceRecord(playerId="p",trainingId="t",status=AttendanceStatus.FALTA,recordedAt=1);val b=a.copy(status=AttendanceStatus.ASISTIO,recordedAt=2);val summary=TeamInsights.attendance(Month(2026,11),listOf(a,b),ts,utc);assertEquals(1,summary.total);assertEquals(100,summary.percent)}
    @Test fun attendanceDoesNotCreateMissingAbsences() {val s=TeamInsights.attendance(Month(2026,11),emptyList(),listOf(Training(id="t",date=date(2026,11,1))),utc);assertEquals(0,s.total);assertNull(s.percent)}
    @Test fun attendanceUnknownDateReportedAndOtherStatusesCounted() {val rows=listOf(AttendanceRecord(playerId="p",trainingId="missing"),AttendanceRecord(playerId="p",trainingId="t",status=AttendanceStatus.LESION));val s=TeamInsights.attendance(Month(2026,11),rows,listOf(Training(id="t",date=date(2026,11,1))),utc);assertEquals(1,s.unregistered);assertEquals(1,s.other);assertEquals(0,s.percent)}
    @Test fun streakUsesOnlyFinalizedMatchesAndOwnHomeAwayScores() {val a=LiveMatch(id="m1",teamA="a",teamB="b",status="finished",date=3);val b=a.copy(id="m2",teamA="b",teamB="a",date=2);val c=a.copy(id="m3",status="in_progress",date=4);val s=TeamInsights.streak("a",listOf(a,b,c),mapOf("m1" to listOf(goal("1")),"m2" to listOf(goal("2")),"m3" to emptyList()));assertEquals(Streak("victorias",2),s)}
    @Test fun streakStopsAtDrawAndIgnoresMissingFeed() {val rows=(1..3).map{LiveMatch(id="$it",teamA="a",teamB="b",status="finished",date=(4-it).toLong())};val s=TeamInsights.streak("a",rows,mapOf("1" to listOf(goal("1")),"2" to emptyList()));assertEquals(Streak("victorias",1),s)}
    @Test fun agendaIncludesBothKindsAndSortsExcludingUnknownDates() {val entries=TeamInsights.agenda(listOf(LiveMatch(id="m",date=20),LiveMatch(id="bad",date=0)),listOf(Training(id="t",date=10)));assertEquals(listOf("training/t","match/m"),entries.map{it.id})}
    @Test fun monthFilterRespectsTimezone() {val value=date(2026,12,1)-12*60*60*1000;assertTrue(Month(2026,11).contains(value,TimeZone.getTimeZone("GMT-05:00")))}
}
