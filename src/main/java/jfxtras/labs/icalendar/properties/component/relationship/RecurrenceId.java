package jfxtras.labs.icalendar.properties.component.relationship;

import java.time.temporal.Temporal;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import jfxtras.labs.icalendar.components.VEventNew;
import jfxtras.labs.icalendar.components.VJournal;
import jfxtras.labs.icalendar.components.VTodo;
import jfxtras.labs.icalendar.parameters.ParameterEnum;
import jfxtras.labs.icalendar.parameters.Range;
import jfxtras.labs.icalendar.parameters.Range.RangeType;
import jfxtras.labs.icalendar.properties.PropertyBaseDateTime;
import jfxtras.labs.icalendar.properties.PropertyRecurrenceID;

/**
 * RECURRENCE-ID
 * RFC 5545, 3.8.4.4, page 112
 * 
 * This property is used in conjunction with the "UID" and
 * "SEQUENCE" properties to identify a specific instance of a
 * recurring "VEVENT", "VTODO", or "VJOURNAL" calendar component.
 * The property value is the original value of the "DTSTART" property
 * of the recurrence instance.
 * 
 * The "RANGE" parameter is used to specify the effective range of
 * recurrence instances from the instance specified by the
 * "RECURRENCE-ID" property value.  The value for the range parameter
 * can only be "THISANDFUTURE"  Note: THISANDFUTURE is not supported by
 * most iCalendar implementations.  It may be better to truncate the
 * unbounded recurring calendar component (i.e., with the "COUNT"
 * or "UNTIL" rule parts), and create two new unbounded recurring
 * calendar components for the future instances.
 * 
 * Example:
 * RECURRENCE-ID;VALUE=DATE:19960401
 * 
 * @author David Bal
 * @see VEventNew
 * @see VTodo
 * @see VJournal
 */
public class RecurrenceId<T extends Temporal> extends PropertyBaseDateTime<T, RecurrenceId<T>> implements PropertyRecurrenceID<T>
{
    /**
     * RANGE
     * Recurrence Identifier Range
     * RFC 5545, 3.2.13, page 23
     * 
     * To specify the effective range of recurrence instances from
     *  the instance specified by the recurrence identifier specified by
     *  the property.
     * 
     * Example:
     * RECURRENCE-ID;RANGE=THISANDFUTURE:19980401T133000Z
     * 
     * @author David Bal
     *
     */
    @Override
    public Range getRange() { return (range == null) ? null : range.get(); }
    @Override
    public ObjectProperty<Range> rangeProperty()
    {
        if (range == null)
        {
            range = new SimpleObjectProperty<>(this, ParameterEnum.RECURRENCE_IDENTIFIER_RANGE.toString());
        }
        return range;
    }
    private ObjectProperty<Range> range;
    @Override
    public void setRange(Range range)
    {
        if (range != null)
        {
            rangeProperty().set(range);
        }
    }
    public void setRange(String value) { setRange(new Range(value)); }
    public RecurrenceId<T> withRange(Range altrep) { setRange(altrep); return this; }
    public RecurrenceId<T> withRange(RangeType value) { setRange(new Range(value)); return this; }
    public RecurrenceId<T> withRange(String content) { setRange(content); return this; }

   public RecurrenceId(T temporal)
    {
        super(temporal);
    }

    public RecurrenceId(Class<T> clazz, CharSequence contentLine)
    {
        super(clazz, contentLine);
    }
    
    public RecurrenceId(RecurrenceId<T> source)
    {
        super(source);
    }
}
