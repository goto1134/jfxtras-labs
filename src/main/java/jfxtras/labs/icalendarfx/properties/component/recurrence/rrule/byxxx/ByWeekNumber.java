package jfxtras.labs.icalendarfx.properties.component.recurrence.rrule.byxxx;

import static java.time.temporal.ChronoUnit.WEEKS;

import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jfxtras.labs.icalendarfx.properties.component.recurrence.rrule.RRuleElementType;

/** BYWEEKNO from RFC 5545, iCalendar 3.3.10, page 42 */
public class ByWeekNumber extends ByRuleAbstract<ObservableList<Integer>, ByWeekNumber>
{
//    /** sorted array of weeks of the year
//     * (i.e. 5, 10 = 5th and 10th weeks of the year, -3 = 3rd from last week of the year)
//     * Uses a varargs parameter to allow any number of value.
//     */
//    public int[] getWeekNumbers() { return weekNumbers; }
//    private int[] weekNumbers;
//    public void setWeekNumbers(int... weekNumbers)
//    {
//        for (int w : weekNumbers)
//        {
//            if (w < -53 || w > 53 || w == 0) throw new IllegalArgumentException("Invalid BYWEEKNO value (" + w + "). Valid values are 1 to 53 or -53 to -1.");
//        }
//        this.weekNumbers = weekNumbers;
//    }
//    public ByWeekNumber withWeekNumbers(int... weekNumbers) { setWeekNumbers(weekNumbers); return this; }
    public void setValue(Integer... weekNumbers)
    {
        for (int w : weekNumbers)
        {
            if (w < -53 || w > 53 || w == 0) throw new IllegalArgumentException("Invalid BYWEEKNO value (" + w + "). Valid values are 1 to 53 or -53 to -1.");
        }
        setValue(FXCollections.observableArrayList(weekNumbers));
    }
    public void setValue(String weekNumbers)
    {
        parseContent(weekNumbers);        
    }
    public ByWeekNumber withValue(Integer... weekNumbers)
    {
        setValue(weekNumbers);
        return this;
    }
    public ByWeekNumber withValue(String weekNumbers)
    {
        setValue(weekNumbers);
        return this;
    }
    
    /** Start of week - default start of week is Monday */
    public DayOfWeek getWeekStart() { return weekStart; }
    private DayOfWeek weekStart = DayOfWeek.MONDAY; // default to start on Monday
    public void setWeekStart(DayOfWeek weekStart) { this.weekStart = weekStart; }
    public ByWeekNumber withWeekStart(DayOfWeek weekStart) { this.weekStart = weekStart; return this; }

    /*
     * CONSTRUCTORS
     */
    public ByWeekNumber()
    {
        super();
//        super(ByWeekNumber.class);
    }
    
//    /** takes String of comma-delimited integers, parses it to array of ints 
//     */
//    public ByWeekNumber(String weekNumbers)
//    {
//        this();
//        parseContent(weekNumbers);
//    }
    
    /** Constructor requires weeks of the year int value(s) */
    public ByWeekNumber(Integer...weekNumbers)
    {
        this();
        setValue(weekNumbers);
    }
    
    public ByWeekNumber(ByWeekNumber source)
    {
        super(source);
    }

//    @Override
//    public void copyTo(ByRule destination)
//    {
//        ByWeekNumber destination2 = (ByWeekNumber) destination;
//        destination2.weekNumbers = new int[weekNumbers.length];
//        for (int i=0; i<weekNumbers.length; i++)
//        {
//            destination2.weekNumbers[i] = weekNumbers[i];
//        }
//    }
    
//    @Override
//    public boolean equals(Object obj)
//    {
//        if (obj == this) return true;
//        if((obj == null) || (obj.getClass() != getClass())) {
//            return false;
//        }
//        ByWeekNumber testObj = (ByWeekNumber) obj;
//        
//        boolean weekNumbersEquals = Arrays.equals(getValue(), testObj.getValue());
//        return weekNumbersEquals;
//    }
//    
//    @Override
//    public int hashCode()
//    {
//        int hash = 7;
//        hash = (31 * hash) + getWeekNumbers().hashCode();
//        return hash;
//    }
    
    @Override
    public String toContent()
    {
        String days = getValue().stream()
                .map(v -> v.toString() + ",")
                .collect(Collectors.joining(","));
        return RRuleElementType.BY_WEEK_NUMBER + "=" + days; //.substring(0, days.length()-1); // remove last comma
    }
    
    @Override
    public Stream<Temporal> streamRecurrences(Stream<Temporal> inStream, ObjectProperty<ChronoUnit> chronoUnit, Temporal startTemporal)
    {
        ChronoUnit originalChronoUnit = chronoUnit.get();
        chronoUnit.set(WEEKS);
        switch (originalChronoUnit)
        {
        case YEARS:
            Locale oldLocale = null;
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            DayOfWeek firstDayOfWeek = weekFields.getFirstDayOfWeek();
            if (firstDayOfWeek != getWeekStart())
            {
                switch (weekStart)
                { // Pick a Locale that matches the first day of week specified.
                case MONDAY:
                    oldLocale = Locale.getDefault();
                    Locale.setDefault(Locale.FRANCE);
                    break;
                case SUNDAY:
                    oldLocale = Locale.getDefault();
                    Locale.setDefault(Locale.US);
                    break;
                case FRIDAY:
                case SATURDAY:
                case THURSDAY:
                case TUESDAY:
                case WEDNESDAY:
                default:
                    throw new RuntimeException("Not implemented start of week " + weekStart);
                }
            }
            WeekFields weekFields2 = WeekFields.of(Locale.getDefault());
            if (weekFields2.getFirstDayOfWeek() != getWeekStart()) throw new RuntimeException("Can't match first day of week " + getWeekStart());

            // Make output stream
            Stream<Temporal> outStream = inStream.flatMap(date -> 
            { // Expand to include matching days in all months
                DayOfWeek dayOfWeek = DayOfWeek.from(startTemporal);
                List<Temporal> dates = new ArrayList<>();
                for (int myWeekNumber: getValue())
                {
                    Temporal newTemporal = date.with(TemporalAdjusters.next(dayOfWeek));
                    int newDateWeekNumber = newTemporal.get(weekFields2.weekOfWeekBasedYear());
                    int weekShift = myWeekNumber - newDateWeekNumber;
                    dates.add(newTemporal.plus(weekShift, ChronoUnit.WEEKS));
                }
                return dates.stream();
            });
            if (oldLocale != null) Locale.setDefault(oldLocale); // if changed, return Locale to former setting
            return outStream;
        case DAYS:
        case WEEKS:
        case MONTHS:
        case HOURS:
        case MINUTES:
        case SECONDS:
            throw new IllegalArgumentException("BYWEEKNO is not available for " + chronoUnit.get() + " frequency."); // Not available
        default:
            break;
        }
        return null;    
    }
    
    @Override
    public void parseContent(String weekNumbers)
    {
        Integer[] weekArray = Arrays.asList(weekNumbers.split(","))
                .stream()
                .map(s -> Integer.parseInt(s))
                .toArray(size -> new Integer[size]);
        setValue(weekArray);
    }

    public static ByWeekNumber parse(String content)
    {
        ByWeekNumber element = new ByWeekNumber();
        element.parseContent(content);
        return element;
    }

}
