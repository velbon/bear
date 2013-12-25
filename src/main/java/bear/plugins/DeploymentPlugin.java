package bear.plugins;

import bear.core.GlobalContext;
import bear.core.SessionContext;
import bear.plugins.misc.PendingRelease;
import bear.plugins.misc.Release;
import bear.plugins.misc.Releases;
import bear.plugins.misc.ReleasesPlugin;
import bear.task.*;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Andrey Chaschev chaschev@gmail.com
 */
public class DeploymentPlugin extends Plugin {
    ReleasesPlugin releases;

    public DeploymentPlugin(GlobalContext global) {
        super(global);
    }

    public Builder newBuilder(){
        return new Builder();
    }

    public static <T extends Builder.DeploymentStep> String stepNameBefore(Class<T> tClass){
        return stepName(tClass, ".before");
    }

    public static <T extends Builder.DeploymentStep> String stepNameMain(Class<T> tClass){
        return stepName(tClass, ".main");
    }

    public static <T extends Builder.DeploymentStep> String stepNameAfter(Class<T> tClass){
        return stepName(tClass, ".after");
    }

    private static <T extends Builder.DeploymentStep> String stepName(Class<T> tClass, String s) {
        return tClass.getSimpleName() + s;
    }

    public class Builder{
        // in before: pendingRelease
        protected final SetFilesLocation setFilesLocation = new SetFilesLocation();
        protected final CheckoutFiles checkoutFiles = new CheckoutFiles();
        protected final BuildAndCopy buildAndCopy = new BuildAndCopy();
        protected final StopService stopService = new StopService();
        protected final UpdateLinks updateLinks = new UpdateLinks();
        // in before: activatedRelease
        protected final StartService startService = new StartService();
        protected final WhenStarted whenStarted = new WhenStarted();
        protected final IfRollback ifRollback = new IfRollback();

        protected final DeploymentStep[] deploymentSteps = new DeploymentStep[]{
            setFilesLocation, checkoutFiles, buildAndCopy,
            stopService, updateLinks, startService,
            whenStarted
        };

        public abstract class DeploymentStep<SELF extends DeploymentStep>  {
            String beforeCallableName = stepNameBefore(getClass());
            String taskCallableName = stepNameMain(getClass());
            String afterCallableName = stepNameAfter(getClass());

            @Nullable
            protected TaskCallable<TaskDef> beforeCallable;
            @Nullable protected TaskCallable<TaskDef> taskCallable;

            @Nullable
            protected TaskCallable<TaskDef> afterCallable;

            protected DeploymentStep(final TaskCallable<TaskDef> taskCallable) {
                this.taskCallable = taskCallable;
            }

            protected DeploymentStep() {
            }

            public SELF setTaskCallable(TaskCallable<TaskDef> taskCallable) {
                this.taskCallable = taskCallable;
                return self();
            }

            public SELF setTaskCallable(String name, TaskCallable<TaskDef> taskCallable) {
                this.taskCallable = taskCallable;
                this.taskCallableName = name;
                return self();
            }

            @SuppressWarnings("unchecked")
            protected final SELF self(){
                return (SELF) this;
            }

            public Builder endDeploy() {
                return Builder.this;
            }

            public List<TaskDef<Task>> createTasksToList(List<TaskDef<Task>> tasks){
                addTask(tasks, beforeCallable, beforeCallableName);
                addTask(tasks, taskCallable, taskCallableName);
                addTask(tasks, afterCallable, afterCallableName);

                return tasks;
            }

            private void addTask(List<TaskDef<Task>> tasks, TaskCallable<TaskDef> callable, String callableName) {
                if(callable == null) return;

                tasks.add(new TaskDef<Task>(callable).setName(callableName));
            }

            public SELF setBeforeCallableName(String beforeCallableName) {
                this.beforeCallableName = beforeCallableName;
                return self();
            }

            public SELF setTaskCallableName(String taskCallableName) {
                this.taskCallableName = taskCallableName;
                return self();
            }

            public SELF setAfterCallableName(String afterCallableName) {
                this.afterCallableName = afterCallableName;
                return self();
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");
                if(taskCallableName != null) sb.append("task='").append(taskCallableName).append('\'');
                if(beforeCallableName != null) sb.append(" before='").append(beforeCallableName).append('\'');
                if(afterCallableName != null) sb.append(" after='").append(afterCallableName).append('\'');
                sb.append('}');
                return sb.toString();
            }
        }

        Builder() {
        }

        public SetFilesLocation $1_SetFilesLocation(){
            return setFilesLocation;
        }

        public CheckoutFiles CheckoutFiles_2(){
            return checkoutFiles;
        }

        public CheckoutFiles CheckoutFiles_2(TaskCallable<TaskDef> t){
            return checkoutFiles.setTaskCallable(t);
        }

        public class SetFilesLocation extends DeploymentStep<SetFilesLocation> {
            public SetFilesLocation() {
                beforeCallable = new TaskCallable<TaskDef>() {
                    @Override
                    public TaskResult call(SessionContext $, Task<TaskDef> task, Object input) throws Exception {
                        $.var(releases.session).newPendingRelease();

                        return TaskResult.OK;
                    }
                };
            }

            public CheckoutFiles $2_CheckoutFiles(){
                return checkoutFiles;
            }

            public BuildAndCopy $3_BuildAndCopy(){
                return buildAndCopy;
            }
        }

        public class CheckoutFiles extends DeploymentStep<CheckoutFiles> {
            public BuildAndCopy BuildAndCopy_3(){
                return buildAndCopy;
            }

            public BuildAndCopy BuildAndCopy_3(TaskCallable<TaskDef> t){
                return buildAndCopy.setTaskCallable(t);
            }
        }

        public class BuildAndCopy extends DeploymentStep<BuildAndCopy> {
            public StopService StopService_5() {
                return stopService;
            }

            public StopService StopService_5(TaskCallable<TaskDef> t) {
                return stopService.setTaskCallable(t);
            }
        }

        public class StopService extends DeploymentStep<StopService> {
            public UpdateLinks $6_UpdateLinks() {
                return updateLinks;
            }

            public StartService StartService_8() {
                return startService;
            }

            public StopService waitForParties(){
                beforeCallable = Tasks.andThen(Task.awaitOthersCallable(bear.appWaitOthersTimeoutSec, SECONDS), beforeCallable);
                return this;
            }

            public StartService StartService_8(TaskCallable<TaskDef> t) {
                return startService.setTaskCallable(t);
            }
        }

        public class UpdateLinks extends DeploymentStep<UpdateLinks> {
            protected Symlinks symlinks = new Symlinks();

            public UpdateLinks addSymlink(SymlinkEntry symlinkEntry) {
                symlinks.add(symlinkEntry);
                return this;
            }
        }

        public class StartService extends DeploymentStep<StartService> {
            public StartService() {
                beforeCallable = new TaskCallable<TaskDef>() {
                    @Override
                    public TaskResult call(SessionContext $, Task<TaskDef> task, Object input) throws Exception {
                        ReleasesPlugin releases = $.getGlobal().plugin(ReleasesPlugin.class);
                        PendingRelease pendingRelease = $.var(releases.pendingRelease);

                        if (pendingRelease != null) {
                            pendingRelease.activate();
                        } else {
                            // a case of standalone start
                            Optional<Release> releaseOptional = $.var(releases.session).getCurrentRelease();

                            if (!releaseOptional.isPresent()) {
                                throw new IllegalStateException("there is no release set!");
                            }
                        }

//                        $.putConst(releases.activatedRelease, Optional.of(activatedRelease));
                        return TaskResult.OK;
                    }
                };

            }

            public WhenStarted $10_WhenStarted() {
                return whenStarted;
            }

        }

        public class WhenStarted extends DeploymentStep<WhenStarted> {

            public WhenStarted() {

            }

            public WhenStarted failIfAnyFails(){
                TaskCallable<TaskDef> waitOthers = new TaskCallable<TaskDef>() {
                    @Override
                    public TaskResult call(SessionContext $, Task<TaskDef> task, Object input) throws Exception {
                        ListenableFuture<List<TaskResult>> prevResults = task.aggregateRelatively(-1, TaskResult.class);

                        List<TaskResult> results = prevResults.get($.var(bear.appWaitOthersTimeoutSec), SECONDS);

                        return Tasks.and(results);
                    }
                };

                beforeCallable = Tasks.andThen(waitOthers, beforeCallable);

                return this;
            }
        }

        public class IfRollback extends DeploymentStep<WhenStarted> {
            protected TaskCallable<TaskDef> beforeLinkSwitch;
            protected TaskCallable<TaskDef> afterLinkSwitch;

            IfRollback() {
            }

            public Builder endRollback() {
                return Builder.this;
            }

            public IfRollback beforeLinkSwitch(TaskCallable<TaskDef> beforeLabelSwitch) {
                this.beforeLinkSwitch = beforeLabelSwitch;
                return this;
            }

            public IfRollback afterLinkSwitch(TaskCallable<TaskDef> afterLabelSwitch) {
                this.afterLinkSwitch = afterLabelSwitch;
                return this;
            }
        }

        public IfRollback ifRollback(){
            return ifRollback;
        }

        public SetFilesLocation getSetFilesLocation() {
            return setFilesLocation;
        }

        public CheckoutFiles getCheckoutFiles() {
            return checkoutFiles;
        }

        public BuildAndCopy getBuildAndCopy() {
            return buildAndCopy;
        }

        public UpdateLinks getUpdateLinks() {
            return updateLinks;
        }

        public WhenStarted getWhenStarted() {
            return whenStarted;
        }

        public DeploymentStep[] getDeploymentSteps() {
            return deploymentSteps;
        }

        //todo each step could provide it's own implementation for the task
        public TaskDef<Task> build(){
            final TaskCallable<TaskDef> ifRollbackCallable = new TaskCallable<TaskDef>() {
                @Override
                public TaskResult call(SessionContext $, Task<TaskDef> task, Object input) throws Exception {
                    Optional<Release> activatedRelease = $.var(releases.activatedRelease);

                    if($.var(releases.manualRollback)){
                        $.var(releases.session).markRollback(activatedRelease.get());
                    }else
                    if(activatedRelease.isPresent()){
                        $.var(releases.session).markFailed(activatedRelease.get());
                    }

                    if(!$.isDefined(releases.rollbackToRelease)) {
                        //won't be defined for i.e. manual start/restart with no build
                        return TaskResult.OK;
                    }

                    TaskResult result = TaskResult.OK;

                    if(ifRollback.beforeLinkSwitch != null){
                        result = ifRollback.beforeLinkSwitch.call($, task, input);
                    }

                    Releases r = $.var(releases.session);

                    Optional<Release> rollbackTo = $.var(releases.rollbackToRelease);

//                    $.putConst(releases.activatedRelease, rollbackTo);

                    if(rollbackTo.isPresent()){
                        r.rollbackTo(rollbackTo.get());
                    }

                    if(ifRollback.afterLinkSwitch != null){
                        result = result.and(ifRollback.afterLinkSwitch.call($, task, input));
                    }

                    return result;
                }
            };



            final List<TaskDef<Task>> taskDefs = new ArrayList<TaskDef<Task>>();

            for (DeploymentStep deploymentStep : deploymentSteps) {
                deploymentStep.createTasksToList(taskDefs);
            }

            TaskDef<Task> rollback = new TaskDef<Task>("deployment rollback", Tasks.newSingleTask(ifRollbackCallable));

            for (TaskDef<Task> taskDef : taskDefs) {
                taskDef.onRollback(rollback);
            }

            return new TaskDef<Task>("deployment multitask", new MultitaskSupplier<Task>() {
                @Override
                public List<Task> createNewSessions(SessionContext $, Task parent) {
                    final List<Task> tasks = new ArrayList<Task>();

                    for (TaskDef<Task> taskDef : taskDefs) {
                        tasks.add(taskDef.singleTaskSupplier().createNewSession($, parent, taskDef));
                    }

                    return tasks;
                }

                @Override
                public List<TaskDef<Task>> getTaskDefs() {
                    return taskDefs;
                }


                @Override
                public int size() {
                    return taskDefs.size();
                }
            }).onRollback(rollback);
        }

        public StopService getStopService() {
            return stopService;
        }

        public StartService getStartService() {
            return startService;
        }

        public IfRollback getIfRollback() {
            return ifRollback;
        }
    }

    public static void main(String[] args) {
        LinkedHashMap map = new LinkedHashMap();

        TaskDef<Task> taskDef = new DeploymentPlugin(null).new Builder()
            .$1_SetFilesLocation()
            .setTaskCallable(new TaskCallable<TaskDef>() {
                @Override
                public TaskResult call(SessionContext $, Task<TaskDef> task, Object input) throws Exception {
                    throw new UnsupportedOperationException("todo .call");
                }
            })
            .$2_CheckoutFiles()
            .setTaskCallable(new TaskCallable<TaskDef>() {
                @Override
                public TaskResult call(SessionContext $, Task<TaskDef> task, Object input) throws Exception {
                    throw new UnsupportedOperationException("todo .call");
                }
            })
            .BuildAndCopy_3()
//            .$4_WaitForParties()
            .StopService_5()
            .$6_UpdateLinks()
            .endDeploy()
            .build();
    }

    @Override
    public InstallationTaskDef<? extends InstallationTask> getInstall() {
        return InstallationTaskDef.EMPTY;
    }


}
