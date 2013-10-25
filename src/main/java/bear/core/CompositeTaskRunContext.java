/*
 * Copyright (C) 2013 Andrey Chaschev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bear.core;

import bear.console.CompositeConsoleArrival;
import bear.main.BearCommandLineConfigurator;
import bear.plugins.Plugin;
import bear.plugins.sh.SystemSession;
import bear.session.*;
import bear.task.*;
import bear.task.exec.TaskExecutionContext;
import bear.vcs.CommandLineResult;
import chaschev.util.Exceptions;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class CompositeTaskRunContext {
    private final GlobalContext global;
    private final TaskDef task;
    private final CompositeConsoleArrival<SessionContext> consoleArrival;

    public final DynamicVariable<Stats> stats;
    public final DynamicVariable<AtomicInteger> arrivedCount = Variables.newVar(new AtomicInteger(0));

    public CompositeTaskRunContext(GlobalContext global, TaskDef task, CompositeConsoleArrival<SessionContext> consoleArrival) {
        this.global = global;
        this.task = task;
        this.consoleArrival = consoleArrival;

        stats = Variables.dynamic(Stats.class).defaultTo(new Stats(consoleArrival.getArrivedEntries().size(), task));
    }

    public CompositeConsoleArrival<SessionContext> getConsoleArrival() {
        return consoleArrival;
    }

    public void addArrival(int i, SessionContext $) {
        consoleArrival.addArrival(i, $);

        DynamicVariable<TaskExecutionContext> execCtx = $.getExecutionContext().rootExecutionContext;

        boolean isOk = execCtx.getDefaultValue().taskResult.ok();

        stats.getDefaultValue().addArrival(isOk);
        stats.fireExternalModification();

        arrivedCount.getDefaultValue().incrementAndGet();
        arrivedCount.fireExternalModification();
    }

    public void submitTasks() {
        List<ListenableFuture<SessionContext>> futures = consoleArrival.getFutures();
        List<SessionContext> $s = consoleArrival.getEntries();

        for (int i = 0; i < $s.size(); i++) {
            final SessionContext $ = $s.get(i);
            final SystemSession sys = $.getSys();
            final TaskRunner runner = $.getRunner();

            final int finalI = i;

            final ListenableFuture<SessionContext> future = global.taskExecutor.submit(new Callable<SessionContext>() {
                @Override
                public SessionContext call() {
                    try {
                        Thread.currentThread().setName($.threadName());


//                        $.sys.connect();

                        if ($.var($.bear.verifyPlugins)) {
                            DependencyResult r = new DependencyResult(Result.OK);

                            for (Plugin<Task, TaskDef<?>> plugin : global.getGlobalPlugins()) {
                                r.join(plugin.checkPluginDependencies());

                                if (!task.isSetupTask()) {
                                    Dependency dependency = plugin.getInstall().newSession($, $.currentTask)
                                        .asInstalledDependency();

                                    TaskResult result = runner.runSession(dependency);

                                    r.join((DependencyResult) result);
                                }
                            }

                            if (r.nok()) {
                                throw new DependencyException(r.toString());
                            }
                        }

                        final TaskResult run = runner.run(task);

                        if (!run.ok()) {
                            System.out.println(run);
                        }

                        $.executionContext.rootExecutionContext.getDefaultValue().taskResult = run;
                    } catch (Throwable e) {
                        BearCommandLineConfigurator.logger.warn("", e);

                        $.executionContext.rootExecutionContext.getDefaultValue().taskResult = new CommandLineResult(Result.ERROR, e.toString());

                        if (e instanceof Error) {
                            throw (Error) e;
                        }

                        throw Exceptions.runtime(e);
                    }finally {
                        try {
                            $.executionContext.rootExecutionContext.fireExternalModification();
                            addArrival(finalI, $);
                        } catch (Exception e) {
                            BearCommandLineConfigurator.logger.warn("", e);
                        }
                    }

                    return $;
                }
            });

            futures.add(future);
        }

        stats.fireExternalModification();
    }

    public boolean isLocalSession() {
        List<SessionContext> $s = consoleArrival.getEntries();

        return $s.size() == 1 && (!$s.get(0).getSys().isRemote());
    }

    public static class Stats{
        public final AtomicInteger partiesArrived = new AtomicInteger();
        public final AtomicInteger partiesOk = new AtomicInteger();
        public int partiesPending;
        public int partiesFailed = 0;
        public final AtomicInteger partiesCount;
        protected TaskDef rootTask;

        public Stats(int count, TaskDef rootTask) {
            partiesPending = count;
            this.rootTask = rootTask;
            partiesCount = new AtomicInteger(count);
        }

        public void addArrival(boolean isOk) {
            partiesArrived.incrementAndGet();
            partiesPending = partiesCount.get() - partiesArrived.get();

            if(isOk){
                partiesOk.incrementAndGet();
            }

            partiesFailed = partiesArrived.get() - partiesOk.get();
        }

        public String getRootTask() {
            return rootTask.getName();
        }
    }

    public GlobalContext getGlobal() {
        return global;
    }

    public TaskDef getTask() {
        return task;
    }

    public int size(){
        return consoleArrival.getEntries().size();
    }
}
