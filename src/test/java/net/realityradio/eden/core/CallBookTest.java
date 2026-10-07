package net.realityradio.eden.core;
import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class CallBookTest {
 @Test void incomingOnlyAnswerAndPeerCleanup(){var b=new CallBook();var a=UUID.randomUUID();var c=UUID.randomUUID();var ad=UUID.randomUUID();var cd=UUID.randomUUID();b.dial(a,c,ad,cd,"1","2",0);assertThrows(IllegalArgumentException.class,()->b.answer(a,ad,1));assertThrows(IllegalArgumentException.class,()->b.answer(c,ad,1));assertTrue(b.answer(c,cd,1).active());assertThrows(IllegalArgumentException.class,()->b.answer(c,cd,2));b.end(c);assertNull(b.get(a));assertNull(b.get(c));}
 @Test void selfCallsAndBusyTargetsAreRejected(){var b=new CallBook();var a=UUID.randomUUID();var c=UUID.randomUUID();var d=UUID.randomUUID();assertThrows(IllegalArgumentException.class,()->b.dial(a,a,a,a,"1","1",0));b.dial(a,c,a,c,"1","2",0);assertThrows(IllegalArgumentException.class,()->b.dial(d,c,d,c,"3","2",1));assertEquals(1,b.all().size());}
}