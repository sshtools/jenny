package com.sshtools.jenny.web;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LoggedExecutorService implements ScheduledExecutorService {
	
	private ScheduledExecutorService delegate;

	public LoggedExecutorService(ScheduledExecutorService delegate) {
		this.delegate = delegate;
	}

	@Override
	public void shutdown() {
		delegate.shutdown();;
	}

	@Override
	public List<Runnable> shutdownNow() {
		return delegate.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return delegate.awaitTermination(timeout, unit);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return delegate.submit(WebLog.logTask(task));
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return delegate.submit(WebLog.logTask(task), result);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return delegate.submit(WebLog.logTask(task));
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return delegate.invokeAll(tasks.stream().map(WebLog::logTask).toList());
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return delegate.invokeAll(tasks.stream().map(WebLog::logTask).toList(), timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return delegate.invokeAny(tasks.stream().map(WebLog::logTask).toList());
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return delegate.invokeAny(tasks.stream().map(WebLog::logTask).toList(), timeout, unit);
	}

	@Override
	public void execute(Runnable command) {
		delegate.execute(WebLog.logTask(command));
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return delegate.schedule(WebLog.logTask(command), delay, unit);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return delegate.schedule(WebLog.logTask(callable), delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return delegate.scheduleAtFixedRate(WebLog.logTask(command),initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return delegate.scheduleWithFixedDelay(WebLog.logTask(command),initialDelay, delay, unit);
	}

}
