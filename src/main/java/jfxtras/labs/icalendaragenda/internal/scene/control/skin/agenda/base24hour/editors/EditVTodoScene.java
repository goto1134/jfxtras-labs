package jfxtras.labs.icalendaragenda.internal.scene.control.skin.agenda.base24hour.editors;

import java.time.temporal.Temporal;
import java.util.List;

import jfxtras.labs.icalendarfx.components.VTodo;
import jfxtras.labs.icalendarfx.properties.component.recurrence.RecurrenceRule;

/**
 * Scene for editing descriptive properties and a {@link RecurrenceRule} in a {@link VTodo}.
 * A {@link EditVTodoTabPane} is set as the root node of the scene graph<br>
 * 
 * @author David Bal
 */
public class EditVTodoScene extends EditDisplayableScene
{
    public EditVTodoScene()
    {
        super(new EditVTodoTabPane());
    }
    
    /**
     * @param vComponent - component to edit
     * @param vComponents - collection of components that vComponent is a member
     * @param startRecurrence - start of selected recurrence
     * @param endRecurrence - end of selected recurrence
     * @param categories - available category names
     */
    public EditVTodoScene(
            VTodo vComponent,
            List<VTodo> vComponents,
            Temporal startRecurrence,
            Temporal endRecurrence,
            List<String> categories)
    {
        this();
        setupData(vComponent, vComponents, startRecurrence, endRecurrence, categories);
    }
    
    EditVTodoScene setupData(
            VTodo vComponent,
            List<VTodo> vComponents,
            Temporal startRecurrence,
            Temporal endRecurrence,
            List<String> categories)
    {
        ((EditVTodoTabPane) getRoot()).setupData(vComponent, vComponents, startRecurrence, endRecurrence, categories);
        return this;
    }
}
 