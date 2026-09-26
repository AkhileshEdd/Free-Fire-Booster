package in.akhilesh.ember;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;
public class MetricsTest {
    @Test public void failedNetworkNeverBecomesZeroLatency() {
        Metrics.NetworkResult r=new Metrics.NetworkResult(Arrays.asList(null,null));
        assertNull(r.median); assertNull(r.variation); assertEquals(2,r.failures);
    }
    @Test public void computesMedianWithoutCountingFailures() {
        Metrics.NetworkResult r=new Metrics.NetworkResult(Arrays.asList(90.0,10.0,null,30.0,50.0));
        assertEquals(40.0,r.median,0.001);assertEquals(50.0,r.variation,0.001);assertEquals(1,r.failures);
    }
    @Test public void oneSampleHasNoVariation() {
        Metrics.NetworkResult r=new Metrics.NetworkResult(Collections.singletonList(42.0));
        assertEquals(42.0,r.median,0.001);assertNull(r.variation);
    }
    @Test public void flagsActualRisks() {assertEquals(5,Metrics.warnings(10,42f,true,false,true).size());}
    @Test public void missingTemperatureIsNotHot() {assertTrue(Metrics.warnings(-1,null,false,true,false).isEmpty());}
    @Test public void durationNeverNegative() {assertEquals(0,Metrics.duration(200,100));assertEquals(500,Metrics.duration(100,600));}
    @Test public void multilineFormulaIsEscaped() {assertTrue(Metrics.safeCell(" =SUM(1,2)\nnext").startsWith("'"));}
    @Test public void csvEscapesQuotesAndPreservesCommas(){assertEquals("\"a,\"\"b\"\"\"",Metrics.csv("a,\"b\""));}
}
