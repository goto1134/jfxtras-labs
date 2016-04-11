package jfxtras.labs.icalendar.property;

import static org.junit.Assert.assertEquals;

import java.time.ZoneOffset;

import org.junit.Test;

import jfxtras.labs.icalendar.properties.component.timezone.TimeZoneOffsetFrom;
import jfxtras.labs.icalendar.properties.component.timezone.TimeZoneOffsetTo;

public class TimeZoneOffsetTest
{
    @Test
    public void canParseTimeZoneOffsetFrom()
    {
        String content = "TZOFFSETFROM:-0500";
        TimeZoneOffsetFrom madeProperty = new TimeZoneOffsetFrom(content);
        assertEquals(content, madeProperty.toContentLine());
        TimeZoneOffsetFrom expectedProperty = new TimeZoneOffsetFrom(ZoneOffset.of("-05:00"));
        assertEquals(expectedProperty, madeProperty);
    }
    
    @Test
    public void canParseTimeZoneOffsetTo()
    {
        String content = "TZOFFSETTO:+0000";
        TimeZoneOffsetTo madeProperty = new TimeZoneOffsetTo(content);
        assertEquals(content, madeProperty.toContentLine());
        TimeZoneOffsetTo expectedProperty = new TimeZoneOffsetTo(ZoneOffset.of("+00:00"));
        assertEquals(expectedProperty, madeProperty);
    }

}