package jfxtras.labs.icalendaragenda.scene.control.agenda;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import jfxtras.internal.scene.control.skin.agenda.AgendaSkin;
import jfxtras.labs.icalendaragenda.internal.scene.control.skin.agenda.base24hour.NewAppointmentDialog;
import jfxtras.labs.icalendaragenda.internal.scene.control.skin.agenda.base24hour.OneAppointmentSelectedAlert;
import jfxtras.labs.icalendaragenda.internal.scene.control.skin.agenda.base24hour.OneSelectedAppointmentPopup;
import jfxtras.labs.icalendaragenda.internal.scene.control.skin.agenda.base24hour.Settings;
import jfxtras.labs.icalendaragenda.internal.scene.control.skin.agenda.base24hour.components.CreateEditComponentPopupScene;
import jfxtras.labs.icalendaragenda.scene.control.agenda.behaviors.Behavior;
import jfxtras.labs.icalendaragenda.scene.control.agenda.behaviors.VEventBehavior;
import jfxtras.labs.icalendaragenda.scene.control.agenda.behaviors.VJournalBehavior;
import jfxtras.labs.icalendaragenda.scene.control.agenda.behaviors.VTodoBehavior;
import jfxtras.labs.icalendaragenda.scene.control.agenda.factories.DefaultRecurrenceFactory;
import jfxtras.labs.icalendaragenda.scene.control.agenda.factories.DefaultVComponentFactory;
import jfxtras.labs.icalendaragenda.scene.control.agenda.factories.RecurrenceFactory;
import jfxtras.labs.icalendaragenda.scene.control.agenda.factories.VComponentFactory;
import jfxtras.labs.icalendarfx.VCalendar;
import jfxtras.labs.icalendarfx.components.VComponent;
import jfxtras.labs.icalendarfx.components.VComponentDisplayable;
import jfxtras.labs.icalendarfx.components.VEvent;
import jfxtras.labs.icalendarfx.components.VJournal;
import jfxtras.labs.icalendarfx.components.VTodo;
import jfxtras.labs.icalendarfx.components.revisors.ChangeDialogOption;
import jfxtras.labs.icalendarfx.utilities.DateTimeUtilities;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.util.NodeUtil;
/**
 * Extension of JFXtras Agenda that uses iCalendar components to make appointments for
 * Agenda to render.
 * 
 * VComponents contains the iCalendar objects.
 * 
 * Appointment rendering:
 * Appointment rendering is handled by Agenda.  Agenda refreshes its rendering of appointments when changes to the
 * appointments ObservableList occur.
 * ICalendarAgenda handles changes to the vComponents list and refreshes refreshed when Agenda's localDateTimeRangeCallback fires.
 * 
 * @author David Bal
 *
 */
public class ICalendarAgenda extends Agenda
{   
    public final static String ICALENDAR_STYLE_SHEET = ICalendarAgenda.class.getResource(ICalendarAgenda.class.getSimpleName() + ".css").toExternalForm();
    
//    private ObjectProperty<LocalDateTime> startRange = new SimpleObjectProperty<>(); // must be updated when range changes
//    private ObjectProperty<LocalDateTime> endRange = new SimpleObjectProperty<>();
    private LocalDateTimeRange dateTimeRange; // date range of current skin, set when localDateTimeRangeCallback fires

    public void setDateTimeRange(LocalDateTimeRange dateTimeRange)
    {
        this.dateTimeRange = dateTimeRange;
        getRecurrenceFactory().setStartRange(dateTimeRange.getStartLocalDateTime());
        getRecurrenceFactory().setEndRange(dateTimeRange.getEndLocalDateTime());
//        getRecurrenceHelper().setStartRange(dateTimeRange.getStartLocalDateTime());
//        getRecurrenceHelper().setEndRange(dateTimeRange.getEndLocalDateTime());
    }
    public LocalDateTimeRange getDateTimeRange() { return dateTimeRange; }
    
//    // Recurrence helper - handles making appointments, edit and delete components
//    @Deprecated
//    final private RecurrenceHelper<Appointment> recurrenceHelper;
//    @Deprecated
//    public RecurrenceHelper<Appointment> getRecurrenceHelper() { return recurrenceHelper; }
    
    public VComponentFactory<Appointment> getVComponentFactory() { return vComponentFactory; }
    private VComponentFactory<Appointment> vComponentFactory;
    public void setVComponentFactory(VComponentFactory<Appointment> vComponentFactory) { this.vComponentFactory = vComponentFactory; }

    public RecurrenceFactory<Appointment> getRecurrenceFactory() { return recurrenceFactory; }
    private RecurrenceFactory<Appointment> recurrenceFactory;
    public void setRecurrenceFactory(RecurrenceFactory<Appointment> recurrenceFactory) { this.recurrenceFactory = recurrenceFactory; }

    
    /** The VCalendar object that contains all scheduling information */
    public VCalendar getVCalendar() { return vCalendar; }
    final private VCalendar vCalendar;
    
    private ObservableList<String> categories; // initialized in constructor
    public ObservableList<String> getCategories() { return categories; }
    public void setCategories(ObservableList<String> categories)
    {
        this.categories = categories;
    }

    /* 
     * Match up maps
     * 
     * map stores start date/time of Appointments as they are made so I can get the original date/time
     * if Agenda changes one (e.g. drag-n-drop).  The original is needed for RECURRENCE-ID.  */
    private final Map<Integer, Temporal> appointmentStartOriginalMap = new HashMap<>();
    public Map<Integer, Temporal> appointmentStartOriginalMap() { return appointmentStartOriginalMap; } // TODO - POPULATE

    // TODO - FIX MEMORY LEAK WITH appointmentVComponentMap
    private final Map<Integer, VComponentDisplayable<?>> appointmentVComponentMap = new HashMap<>(); /* map matches appointment to VEvent that made it */
    public Map<Integer, VComponentDisplayable<?>> appointmentVComponentMap() { return appointmentVComponentMap; }

    private final Map<Integer, List<Appointment>> vComponentAppointmentMap = new HashMap<>(); /* map matches VComponent to their appointments */

    private final Map<Class<? extends VComponent>, Behavior> vComponentClassBehaviorMap = new HashMap<>();
    

    /** Callback for creating unique identifier values
     * @see VComponent#getUidGeneratorCallback() */
    public Callback<Void, String> getUidGeneratorCallback() { return uidGeneratorCallback; }
    private static Integer nextKey = 0;
    private Callback<Void, String> uidGeneratorCallback = (Void) ->
    { // default UID generator callback
        String dateTime = DateTimeUtilities.LOCAL_DATE_TIME_FORMATTER.format(LocalDateTime.now());
        String domain = "jfxtras.org";
        return dateTime + "-" + nextKey++ + domain;
    };
    public void setUidGeneratorCallback(Callback<Void, String> uidCallback) { this.uidGeneratorCallback = uidCallback; }
    
    // I/O callbacks, must be set to provide I/O functionality, null by default - TODO - NOT IMPLEMENTED YET
    private Callback<Collection<VComponentDisplayable<?>>, Void> repeatWriteCallback = null;
    public void setRepeatWriteCallback(Callback<Collection<VComponentDisplayable<?>>, Void> repeatWriteCallback) { this.repeatWriteCallback = repeatWriteCallback; }

    private Callback<Collection<AppointmentGroup>, Void> appointmentGroupWriteCallback = null; // TODO - NOT IMPLEMENTED YET
    public void setAppointmentGroupWriteCallback(Callback<Collection<AppointmentGroup>, Void> appointmentWriteCallback) { this.appointmentGroupWriteCallback = appointmentGroupWriteCallback; }

    /*
     * APPOINTMENT AND VCOMPONENT LISTENERS 
     * Keeps appointments and vComponents synchronized.
     * listen for additions to appointments from agenda. This listener must be removed and added back when a change
     * in the time range  occurs.
     */
    public ListChangeListener<Appointment> appointmentsListChangeListener;
    public void setAppointmentsListChangeListener(ListChangeListener<Appointment> listener) { appointmentsListChangeListener = listener; }
    public ListChangeListener<Appointment> getAppointmentsListChangeListener() { return appointmentsListChangeListener; }
    
    private ListChangeListener<VComponentDisplayable<?>> vComponentsChangeListener; // listen for changes to vComponents.
    public void setVComponentsChangeListener(ListChangeListener<VComponentDisplayable<?>> listener) { vComponentsChangeListener = listener; }
    public ListChangeListener<VComponentDisplayable<?>> getVComponentsChangeListener() { return vComponentsChangeListener; }

    /*
     * Callback for determining scope of edit change - defaults to always answering ALL
     * Add For choice dialog, change to different callback
     */
    private Callback<Map<ChangeDialogOption, Pair<Temporal,Temporal>>, ChangeDialogOption> oneAllThisAndFutureDialogCallback = (m) -> ChangeDialogOption.ALL;
    public void setOneAllThisAndFutureDialogCallback(Callback<Map<ChangeDialogOption, Pair<Temporal,Temporal>>, ChangeDialogOption> callback) { oneAllThisAndFutureDialogCallback = callback; }
    
    // Default edit popup callback - this callback replaces Agenda's default edit popup
    // It has controls for repeatable events
    private Callback<Appointment, Void> iCalendarEditPopupCallback = (Appointment appointment) ->
    {
        VComponentDisplayable<?> vComponent = appointmentVComponentMap.get(System.identityHashCode(appointment));
        if (vComponent == null)
        {
            // NOTE: Can't throw exception here because in Agenda there is a mouse event that isn't consumed.
            // Throwing an exception will leave the mouse unresponsive.
            System.out.println("ERROR: no component found - popup can'b be displayed");
        } else
        {
            // make popup
            Stage popupStage = new Stage();
            CreateEditComponentPopupScene popupScene = vComponentClassBehaviorMap.get(vComponent.getClass()).getEditScene(appointment);
            popupStage.setScene(popupScene);
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setResizable(false);
            
//            EditComponentPopupStage<?> editPopup = editStage(appointment);

            popupStage.getScene().getStylesheets().addAll(getUserAgentStylesheet(), ICalendarAgenda.ICALENDAR_STYLE_SHEET);

            // remove listeners during edit (to prevent creating extra vEvents when making appointments)
//            popupStage.setOnShowing((windowEvent) -> appointments().removeListener(appointmentsListChangeListener));
            
            /* POSITION POPUP
             * Position popup to left or right of bodyPane, where there is room.
             * Note: assumes the control is displayed at its preferred height and width */
            Pane bodyPane = (Pane) ((AgendaSkin) getSkin()).getNodeForPopup(appointment);
            double prefHeightControl = ((Control) popupStage.getScene().getRoot()).getPrefHeight();
            double prefWidthControl = ((Control) popupStage.getScene().getRoot()).getPrefWidth();
            double xLeft = NodeUtil.screenX(bodyPane) - prefWidthControl - 5;
            double xRight = NodeUtil.screenX(bodyPane) + bodyPane.getWidth() + 5;
            double x = (xLeft > 0) ? xLeft : xRight;
            double y = NodeUtil.screenY(bodyPane) - prefHeightControl/2;
            popupStage.setX(x);
            popupStage.setY(y);
            popupStage.show();
            
            popupScene.getEditDisplayableTabPane().isFinished().addListener((obs) -> popupStage.hide());
            // return listener after edit
//            popupStage.setOnHiding((windowEvent) ->  appointments().addListener(appointmentsListChangeListener));
//            vComponentClassBehaviorMap.get(vComponent.getClass()).iCalendarEditBehavior(appointment);
        }
        return null;
    };
    public Callback<Appointment, Void> getICalendarEditPopupCallback() { return iCalendarEditPopupCallback; }

    /** selectOneAppointmentCallback:
     * When one appointment is selected this callback is run.  It can be used to open a popup to provide edit,
     * delete or other edit options.
     */
    public Callback<Appointment, Void> getSelectedOneAppointmentCallback() { return selectedOneAppointmentCallback; }
    private Callback<Appointment, Void> selectedOneAppointmentCallbackOld = (Appointment appointment) ->
    {
        Pane bodyPane = (Pane) ((AgendaSkin) getSkin()).getNodeForPopup(appointment);
//        SelectedOneAppointmentLoader oneSelectedPopup = new SelectedOneAppointmentLoader(this, appointment);
        OneSelectedAppointmentPopup oneSelectedPopup = new OneSelectedAppointmentPopup();
        oneSelectedPopup.setupData(this, appointment);
//        oneSelectedPopup.isFinished().addListener((obs) -> oneSelectedPopup.hide());
        oneSelectedPopup.show(bodyPane, NodeUtil.screenX(bodyPane) + bodyPane.getWidth()/2, NodeUtil.screenY(bodyPane) + bodyPane.getHeight()/2);
        oneSelectedPopup.focusedProperty().addListener((obs) -> oneSelectedPopup.hide());
        return null;
    };

    Alert lastOneAppointmentSelectedAlert;
    private Callback<Appointment, Void> selectedOneAppointmentCallback = (Appointment appointment) ->
    {
        OneAppointmentSelectedAlert alert = new OneAppointmentSelectedAlert(appointment, Settings.resources);

        alert.initOwner(this.getScene().getWindow());
        Pane bodyPane = (Pane) ((AgendaSkin) getSkin()).getNodeForPopup(appointment);
        alert.setX(NodeUtil.screenX(bodyPane) + bodyPane.getWidth()/2);
        alert.setY(NodeUtil.screenY(bodyPane) + bodyPane.getHeight()/2);
//        System.out.println(alert.getX() + " " + alert.getY());
        
        // Check if previous alert so it can be closed (like autoHide for popups)
        if (lastOneAppointmentSelectedAlert != null)
        {
            lastOneAppointmentSelectedAlert.close();
        }
        lastOneAppointmentSelectedAlert = alert; // save for next time

        alert.resultProperty().addListener((obs, oldValue, newValue) -> 
        {
            if (newValue != null)
            {
                lastOneAppointmentSelectedAlert = null;
                String buttonText = newValue.getText();
                if (buttonText.equals(Settings.resources.getString("edit")))
                {
                    getEditAppointmentCallback().call(appointment);
                } else if (buttonText.equals(Settings.resources.getString("delete")))
                {
                    // TODO - delete
                }
            }
        });
        
        alert.show(); // NOTE: alert.showAndWait() doesn't work - results in a blank dialog panel for 2nd Alert and beyond
        return null;
    };
    public void setSelectedOneAppointmentCallback(Callback<Appointment, Void> c) { selectedOneAppointmentCallback = c; }

    /*
     * Default simple edit popup that opens after new appointment is created.
     * For example, this is done by drawing an appointment in Agenda.
     * allows editing summary and buttons to save and open regular edit popup
     * 
     * to skip the callback, replace with a stub that always returns ButtonData.OK_DONE
     */
    private Callback<Appointment, ButtonData> newAppointmentDrawnCallback = (Appointment appointment) ->
    {
            // TODO - CAN I REMOVE RETURN ARGUMENT FROM CALLBACK - IT WOULD BE CONSISTENT WITH OTHERS
        Dialog<ButtonData> newAppointmentDialog = new NewAppointmentDialog(appointment, appointmentGroups(), Settings.resources);
        newAppointmentDialog.getDialogPane().getStylesheets().add(getUserAgentStylesheet());
        Optional<ButtonData> result = newAppointmentDialog.showAndWait();
        ButtonData button = result.isPresent() ? result.get() : ButtonData.CANCEL_CLOSE;
        return button;
    };
    public Callback<Appointment, ButtonData> getNewAppointmentDrawnCallback() { return newAppointmentDrawnCallback; }
    public void setNewAppointmentDrawnCallback(Callback<Appointment, ButtonData> c) { newAppointmentDrawnCallback = c; }
        
    /*
     * Default changed appointment callback (handles drag-n-drop and expand end time)
     * allows dialog to prompt user for change to all, this-and-future or all for repeating VComponents
     */
    private Callback<Appointment, Void> appointmentChangedCallback = (Appointment appointment) ->
    {
        VComponentDisplayable<?> vComponent = appointmentVComponentMap.get(System.identityHashCode(appointment));
        
        System.out.println("about to revise:" + appointmentVComponentMap.size());
        Behavior behavior = vComponentClassBehaviorMap.get(vComponent.getClass());
        behavior.callRevisor(appointment);
        // TODO - MAKE REVISOR HANDLE EDIT ONE - NEED TO ADD EXDATE
        appointmentStartOriginalMap.put(System.identityHashCode(appointment), appointment.getStartTemporal()); // update start map
        Platform.runLater(() -> refresh());
//        getVCalendar().getVEvents().forEach(System.out::println);
        return null;
    };
    
    // CONSTRUCTOR
    public ICalendarAgenda(VCalendar vCalendar)
    {
        super();
        this.vCalendar = vCalendar;
//        vComponentStore = new DefaultVComponentFromAppointment(appointmentGroups()); // default VComponent store - for Appointments, if other implementation used make new store
        recurrenceFactory = new DefaultRecurrenceFactory(appointmentGroups()); // default recurrence factory - for Appointments, if other implementation is used assign different factory
        vComponentFactory = new DefaultVComponentFactory(); // default VComponent factory - for ppointments
        
        // Populate component class to behavior map with required behaviors
        vComponentClassBehaviorMap.put(VEvent.class, new VEventBehavior(this));
        vComponentClassBehaviorMap.put(VJournal.class, new VJournalBehavior(this));
        vComponentClassBehaviorMap.put(VTodo.class, new VTodoBehavior(this));

//        getVCalendar().getVEvents().addListener((InvalidationListener) (obs) -> 
//        {
////            System.out.println("vComponents chagned:******************************" + vComponents.size());
////            vComponents.stream().forEach(System.out::println);
//        });
        
        // setup default categories from appointment groups
        categories = FXCollections.observableArrayList(
                appointmentGroups().stream()
                    .map(a -> a.getDescription())
                    .collect(Collectors.toList())
                    );
        
        // update appointmentGroup descriptions with changed categories
        getCategories().addListener((ListChangeListener.Change<? extends String> change) ->
        {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    change.getAddedSubList().forEach(c ->
                    {
                        int index = change.getList().indexOf(c);
                        appointmentGroups().get(index).setDescription(c);
                        System.out.println("updated apointmentgroup: " + index + " " + c);
                    });
                }
            }
        });

        // setup event listener to delete selected appointments when Agenda is added to a scene
        sceneProperty().addListener((obs, oldValue, newValue) ->
        {
            if (newValue != null)
            {
                getScene().setOnKeyPressed((event) ->
                {
                    if (event.getCode().equals(KeyCode.DELETE) && (! selectedAppointments().isEmpty()))
                    {
//                        VComponent<?> v = appointmentVComponentMap.get(System.identityHashCode(selectedAppointments().get(0)));
                        appointments().removeAll(selectedAppointments());
                    }
                });
            }
        });

        // setup i18n resource bundle
        Locale myLocale = Locale.getDefault();
        ResourceBundle resources = ResourceBundle.getBundle("jfxtras.labs.icalendaragenda.ICalendarAgenda", myLocale);
        Settings.setup(resources);
        
        setAppointmentChangedCallback(appointmentChangedCallback);

        // Ensures VComponent are synched with appointments.
        // Are assigned here instead of when defined because it removes the vComponentsListener
        // which can't be done before its defined.
        appointmentsListChangeListener = (ListChangeListener.Change<? extends Appointment> change) ->
        {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    if (change.getAddedSubList().size() == 1)
                    { // Open little popup - edit, delete
//                        ZonedDateTime created = ZonedDateTime.now(ZoneId.of("Z"));
                        Appointment appointment = change.getAddedSubList().get(0);
                        String originalSummary = appointment.getSummary();
                        AppointmentGroup originalAppointmentGroup = appointment.getAppointmentGroup();
                        ButtonData button = newAppointmentDrawnCallback.call(change.getAddedSubList().get(0));
                        System.out.println("buttonDate:" + button);
                        switch (button)
                        {
                        case CANCEL_CLOSE:
//                            appointments().remove(appointment);
//                            Platform.runLater(() -> refresh());
                            break;
                        case OK_DONE:
                        {
                            VComponent newVComponent = getVComponentFactory().createVComponent(appointment);
                            getVCalendar().addVComponent(newVComponent);
                            getVCalendar().getVEvents().forEach(System.out::println);
//                            boolean hasSummary = appointment.getSummary() != null;
//                            boolean hasSummaryChanged = appointment.getSummary().equals(originalSummary);
//                            boolean hasAppointmentGroupChanged = appointment.getAppointmentGroup().equals(originalAppointmentGroup);
//                            if ((hasSummary && ! hasSummaryChanged) || ! hasAppointmentGroupChanged)
//                            {
//                                Platform.runLater(() -> refresh());
//                            }
                            break;
                        }
                        case OTHER: // ADVANCED EDIT
                        {
                            VComponent newVComponent = getVComponentFactory().createVComponent(appointment);
                            getVCalendar().addVComponent(newVComponent);
                            iCalendarEditPopupCallback.call(vComponentAppointmentMap.get(System.identityHashCode(newVComponent)).get(0));
//                            Platform.runLater(() -> refresh());
                            break;
                        }
                        default:
                            throw new RuntimeException("unknown button type:" + button);
                        }
                        // remove drawn appointment - if not canceled, it was replaced with one made by the new VComponent
                        appointments().remove(appointment);

//                        if (button == ButtonData.OTHER) // edit appointment
//                        {
//                            iCalendarEditPopupCallback.call(appointment);
//                        }
                    } else throw new RuntimeException("Adding multiple appointments at once is not supported (" + change.getAddedSubList().size() + ")");
                }
                if (change.wasRemoved())
                {
                    change.getRemoved().stream().forEach(a -> 
                    { // add appointments to EXDATE
//                        VComponentDisplayable<?> v = appointmentVComponentMap().get(a);
//                        System.out.println("remove:" + a.hashCode());
//                        Platform.runLater(() -> refresh());
                    });
                }
            }
        };

        // fires when VComponents are added or removed
        // TODO - make generic for VEVENT, VTODO, VJOURNAL
        vComponentsChangeListener = (ListChangeListener.Change<? extends VComponentDisplayable<?>> change) ->
        {
//            System.out.println("vcomponents changed:" + getVCalendar().getVEvents().size());
            while (change.next())
            {
                if (change.wasAdded()) // can't make appointment if range is not set
                {
                    // Check if all VComponets are valid, throw exception otherwise
                    change.getAddedSubList()
                            .stream()
                            .forEach(v -> 
                            {
                                if (! v.isValid())
                                {
                                    throw new RuntimeException("Invalid VComponent:" + System.lineSeparator() + 
                                            v.errors().stream().collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator() +
                                            v.toContent());
                                }
                            });
//                    System.out.println("was added:" + dateTimeRange + " " +  getDateTimeRange());
                    if (dateTimeRange != null)
                    {
//                        Temporal start = getDateTimeRange().getStartLocalDateTime();
//                        Temporal end = getDateTimeRange().getEndLocalDateTime();
                        List<Appointment> newAppointments = new ArrayList<>();
                        // add new appointments
                        change.getAddedSubList()
                                .stream()
                                .forEach(v -> newAppointments.addAll(makeAppointments(v)));
                        appointments().removeListener(appointmentsListChangeListener);
//                        appointments().clear();
//                        System.out.println("About to add");
                        appointments().addAll(newAppointments);
                        appointments().addListener(appointmentsListChangeListener);
//                        refresh();
                    }
                } else if (change.wasRemoved())
                {
                    // remove associated appointments
                    change.getRemoved()
                        .stream()
                        .forEach(v -> 
                        {
                            List<Appointment> remove = vComponentAppointmentMap.get(System.identityHashCode(v));
                            appointments().removeAll(remove);
                        });
                }
            }
        };

        // Listen for changes to appointments (additions and deletions)
        appointments().addListener(appointmentsListChangeListener);

        // Listen for changes to vComponents (additions and deletions)
        getVCalendar().getVEvents().addListener(vComponentsChangeListener);

        /*
         * Open select-one appointment popup
         * listen for changes to selectedAppointments, if only one run callback.
         */
        ListChangeListener<Appointment> selectedAppointmentListener = (ListChangeListener.Change<? extends Appointment> change) ->
        {
            while (change.next())
            {
                if (change.wasAdded() && (selectedAppointments().size() == 1))
                {
                    Appointment appointment = selectedAppointments().get(0);
                    getSelectedOneAppointmentCallback().call(appointment);
                }
            }
        };
        selectedAppointments().addListener(selectedAppointmentListener);
        
        // CHANGE DEFAULT EDIT POPUP - replace default popup with one with repeat options
        setEditAppointmentCallback(iCalendarEditPopupCallback);

        // LISTEN FOR AGENDA RANGE CHANGES
        setLocalDateTimeRangeCallback(dateTimeRange ->
        {
            List<Appointment> newAppointments = new ArrayList<>();
            setDateTimeRange(dateTimeRange);
            getRecurrenceFactory().setStartRange(dateTimeRange.getStartLocalDateTime());
            getRecurrenceFactory().setEndRange(dateTimeRange.getEndLocalDateTime());
//            System.out.println("range0:" + dateTimeRange);
            if (dateTimeRange != null)
            {
                appointments().removeListener(appointmentsListChangeListener); // remove appointmentListener to prevent making extra vEvents during refresh
//                System.out.println("appointments:" + appointments());
                appointments().clear();
                vComponentAppointmentMap.clear();
                appointmentStartOriginalMap.clear();
                appointmentVComponentMap.clear();
                getVCalendar().getVEvents().stream().forEach(v -> newAppointments.addAll(makeAppointments(v)));
                getVCalendar().getVTodos().stream().forEach(v -> newAppointments.addAll(makeAppointments(v)));
                getVCalendar().getVJournals().stream().forEach(v -> newAppointments.addAll(makeAppointments(v)));
                appointments().addAll(newAppointments);
                appointments().addListener(appointmentsListChangeListener); // add back appointmentListener
            }
            return null; // return argument for the Callback
        });
    } // end of constructor
    
    @Override
    public void refresh()
    {
//        List<Appointment> newAppointments = new ArrayList<>();
//        appointments().removeListener(appointmentsListChangeListener); // remove appointmentListener to prevent making extra vEvents during refresh
////        System.out.println("appointments:" + appointments());
//        appointments().clear();
//        vComponentAppointmentMap.clear();
//        appointmentStartOriginalMap.clear();
//        appointmentVComponentMap.clear();
//        getVCalendar().getVEvents().stream().forEach(v -> newAppointments.addAll(makeAppointments(v)));
//        getVCalendar().getVTodos().stream().forEach(v -> newAppointments.addAll(makeAppointments(v)));
//        getVCalendar().getVJournals().stream().forEach(v -> newAppointments.addAll(makeAppointments(v)));
//        appointments().addAll(newAppointments);
//        appointments().addListener(appointmentsListChangeListener); // add back appointmentListener
        getVCalendar().getVEvents().stream().forEach(v -> System.out.println("v:" + v.getUniqueIdentifier() + " " + v.childComponents().size()));
        System.out.println("REFRESH");
//        childComponents()
        super.refresh();
    }
    
    private Collection<Appointment> makeAppointments(VComponentDisplayable<?> v)
    {
        List<Appointment> myAppointments = getRecurrenceFactory().makeRecurrences(v);
        myAppointments.forEach(a -> 
        {
            appointmentVComponentMap.put(System.identityHashCode(a), v);
            appointmentStartOriginalMap.put(System.identityHashCode(a), a.getStartTemporal());
        });
        vComponentAppointmentMap.put(System.identityHashCode(v), myAppointments);
        return myAppointments;
    }  
}
