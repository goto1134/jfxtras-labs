package jfxtras.labs.icalendar.properties.component.descriptive;

import jfxtras.labs.icalendar.components.VEvent;
import jfxtras.labs.icalendar.components.VJournal;
import jfxtras.labs.icalendar.components.VTodo;
import jfxtras.labs.icalendar.properties.PropertyBase;
import jfxtras.labs.icalendar.properties.component.descriptive.Status.StatusType;

/**
 * STATUS
 * RFC 5545 iCalendar 3.8.1.11. page 92
 * 
 * This property defines the overall status or confirmation for the calendar component.
 * 
 * Example:
 * STATUS:TENTATIVE
 *
 * @author David Bal
 * 
 * The property can be specified in following components:
 * @see VEvent
 * @see VTodo
 * @see VJournal
 */
public class Status extends PropertyBase<Status, StatusType>
{
    private String unknownValue; // contains exact string for unknown property value

    public Status(String contentLine)
    {
        super(contentLine);
    }
    
    public Status(StatusType value)
    {
        super(value);
    }
    
    public Status(Status source)
    {
        super(source);
    }
    
    public Status()
    {
        super();
    }
    
    @Override
    protected StatusType valueFromString(String propertyValueString)
    {
        StatusType type = StatusType.valueOf2(propertyValueString);
        if (type == StatusType.UNKNOWN)
        {
            unknownValue = propertyValueString;
        }
        return type;
    }
    
    @Override
    protected String valueToString(StatusType value)
    {
        if (value == StatusType.UNKNOWN)
        {
            return unknownValue;
        }
        return getValue().toString();
    }
    
    public enum StatusType
    {
        TENTATIVE ("TENTATIVE"),
        CONFIRMED ("CONFIRMED"),
        CANCELLED ("CANCELLED"),
        NEEDS_ACTION ("NEEDS-ACTION"),
        COMPLETED ("COMPLETED"),
        IN_PROCESS ("IN-PROCESS"),
        DRAFT ("DRAFT"),
        FINAL ("FINAL"),
        UNKNOWN ("UNKNOWN");
        
        private String name;
        @Override public String toString() { return name; }
        StatusType(String name)
        {
            this.name = name;
        }
        
        static StatusType valueOf2(String value)
        {
            try
            {
                return valueOf(value);
            } catch (IllegalArgumentException e)
            {
                return UNKNOWN;
            }
        }
    }

}
