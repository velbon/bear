package bear.task;

import bear.console.AbstractConsoleCommand;
import bear.core.SessionContext;
import bear.plugins.HavingContext;
import bear.task.exec.TaskExecutionContext;
import bear.vcs.CommandLineResult;

import javax.annotation.Nullable;

/**
 * @author Andrey Chaschev chaschev@gmail.com
 */
public abstract class Task extends HavingContext<Task> {
    protected TaskDef definition;

    @Nullable
    protected final Task parent;

    private Dependencies dependencies = new Dependencies();

    protected TaskExecutionContext executionContext;

    public Task(Task parent, TaskDef definition, SessionContext $) {
        super($);

        this.parent = parent;
        this.definition = definition;

        executionContext = new TaskExecutionContext($);
    }

    public TaskResult run(TaskRunner runner){
        if(parent != null){
            parent.executionContext.onNewSubTask(this);
        }

        TaskResult result = exec(runner);

        executionContext.taskResult = result;

        if(parent != null){
            parent.executionContext.onEndSubTask(this, result);
        }

        return result;
    }

    protected abstract TaskResult exec(TaskRunner runner) ;

    private static final Task NOP_TASK = new Task(null, null, null) {
        @Override
        protected TaskResult exec(TaskRunner runner) {
            return TaskResult.OK;
        }
    };

    public static Task nop() {
        return NOP_TASK;
    }

    public Dependencies getDependencies(){
        return dependencies;
    }

    protected void onRollback() {
        //todo use it
    }

    @Override
    public String toString() {
        return definition == null ? getClass().getSimpleName() :

            definition.getDisplayName();
    }

    public Task addDependency(Dependency... dependencies) {
        this.dependencies.addDependencies(dependencies);
        return this;
    }

    public <T extends CommandLineResult> void onCommandExecutionStart(AbstractConsoleCommand<T> command) {
        executionContext.onNewCommand(command);
    }

    public <T extends CommandLineResult> void onCommandExecutionEnd(AbstractConsoleCommand<T> command, T result) {
        executionContext.onEndCommand(command, result);
    }

    @Nullable
    public Task getParent() {
        return parent;
    }

    public boolean isRootTask(){
        return parent == null;
    }

    public TaskExecutionContext getExecutionContext() {
        return executionContext;
    }

}
