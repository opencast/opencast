package org.opencast.engage.videodisplay.cases.util
{
	import org.flexunit.Assert;
	import org.opencast.engage.videodisplay.control.util.TimeCode;
	
	public class TestTimeCode
	{
	
	 private var timeCode:TimeCode;
	
		 [Before]
        public function setUp():void
        {   
        	timeCode = new TimeCode();
        }

        [After]
        public function tearDown():void
        {
     		timeCode = null;
        }
   
        [Test]
        public function testTimeCode_getTC():void
        {
        var timecode:String = "20:01:03";
          timeCode.getTC(72000);
          Assert.assertEquals( timecode, timeCode.getTC(72063));
        }

	}
}