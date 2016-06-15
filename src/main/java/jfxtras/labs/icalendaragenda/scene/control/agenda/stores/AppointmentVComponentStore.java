package jfxtras.labs.icalendaragenda.scene.control.agenda.stores;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Optional;

import jfxtras.labs.icalendarfx.VCalendar;
import jfxtras.labs.icalendarfx.components.VComponentDisplayable;
import jfxtras.labs.icalendarfx.components.VComponentLocatable;
import jfxtras.labs.icalendarfx.components.VComponentRepeatable;
import jfxtras.labs.icalendarfx.components.VEvent;
import jfxtras.labs.icalendarfx.components.VTodo;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.Appointment;
import jfxtras.scene.control.agenda.Agenda.AppointmentGroup;

public class AppointmentVComponentStore extends VComponentBaseStore<Appointment>
{
    private Collection<AppointmentGroup> appointmentGroups;

    /** Callback to make Appointment from VComponent and start Temporal */
    @Override
    CallbackTwoParameters<VComponentRepeatable<?>, Temporal, Appointment> recurrenceCallBack()
    {
        return (vComponent, startTemporal) ->
        {
            Boolean isWholeDay = vComponent.getDateTimeStart().getValue() instanceof LocalDate;
            VComponentLocatable<?> vComponentLocatable = (VComponentLocatable<?>) vComponent;
            final TemporalAmount adjustment = vComponentLocatable.getActualDuration();
            Temporal endTemporal = startTemporal.plus(adjustment);
    
            /* Find AppointmentGroup
             * control can only handle one category.  Checks only first category
             */
            final AppointmentGroup appointmentGroup;
            if (vComponentLocatable.getCategories() != null)
            {
                String firstCategory = vComponentLocatable.getCategories().get(0).getValue().get(0);
                Optional<AppointmentGroup> myGroup = appointmentGroups
                        .stream()
                        .filter(g -> g.getDescription().equals(firstCategory))
                        .findAny();
                appointmentGroup = (myGroup.isPresent()) ? myGroup.get() : null;
            } else
            {
                appointmentGroup = null;
            }
            // Make appointment
            Appointment appt = new Agenda.AppointmentImplTemporal()
                    .withStartTemporal(startTemporal)
                    .withEndTemporal(endTemporal)
                    .withDescription( (vComponentLocatable.getDescription() != null) ? vComponentLocatable.getDescription().getValue() : null )
                    .withSummary( (vComponentLocatable.getSummary() != null) ? vComponentLocatable.getSummary().getValue() : null)
                    .withLocation( (vComponentLocatable.getLocation() != null) ? vComponentLocatable.getLocation().getValue() : null)
                    .withWholeDay(isWholeDay)
                    .withAppointmentGroup(appointmentGroup);
            return appt;
        };
    }
            
    public AppointmentVComponentStore(Collection<AppointmentGroup> appointmentGroups)
    {
        super();
        this.appointmentGroups = appointmentGroups;
    }

    @Override
    public VComponentDisplayable<?> createVComponent(Appointment appointment, VCalendar vCalendar)
    {
        final VComponentDisplayable<?> newVComponent;
        ZonedDateTime dtCreated = ZonedDateTime.now(ZoneId.of("Z"));
        boolean hasEnd = appointment.getEndTemporal() != null;
        if (hasEnd)
        {
            newVComponent = new VEvent()
                    .withSummary(appointment.getSummary())
                    .withCategories(appointment.getAppointmentGroup().getDescription())
                    .withDateTimeStart(appointment.getStartTemporal())
                    .withDateTimeEnd(appointment.getEndTemporal())
                    .withDescription(appointment.getDescription())
                    .withLocation(appointment.getLocation())
                    .withDateTimeCreated(dtCreated)
                    .withDateTimeStamp(dtCreated)
                    .withUniqueIdentifier();
            vCalendar.getVEvents().add((VEvent) newVComponent);
        } else
        {
            newVComponent = new VTodo()
                    .withSummary(appointment.getSummary())
                    .withCategories(appointment.getAppointmentGroup().getDescription())
                    .withDateTimeStart(appointment.getStartTemporal())
                    .withDescription(appointment.getDescription())
                    .withLocation(appointment.getLocation())
                    .withDateTimeCreated(dtCreated)
                    .withDateTimeStamp(dtCreated)
                    .withUniqueIdentifier();
            vCalendar.getVTodos().add((VTodo) newVComponent);
        }
        return newVComponent;
    }
}
