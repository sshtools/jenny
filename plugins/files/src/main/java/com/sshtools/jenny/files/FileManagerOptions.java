package com.sshtools.jenny.files;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.sshtools.jenny.api.Lang;

public final class FileManagerOptions {
	
	public enum Action {
		NEW_FOLDER,
		DELETE,
		CUT,
		COPY,
		PASTE,
		DOWNLOAD,
		UPLOAD
	}
	
	public enum NavAction {
		UP,
		HOME,
		ROOT,
		BREADCRUMBS,
		NEW_FOLDER,
		CD;
		
		public static NavAction[] forBrowser() {
			return new NavAction[] { UP, HOME, ROOT, BREADCRUMBS, CD };
		}
		
		public static NavAction[] forLocationBar() {
			return new NavAction[] { UP, HOME, ROOT, NEW_FOLDER };
		}
	}
	
	public enum Component {
		TOOLBAR,
		NAVIGATION,
		SEARCH,
		OPTIONS,
		LOCATION_BAR
	}
	
	public enum SelectionMode {
		SINGLE, MULTIPLE
	}
	
	public enum Mode {
		FILES, DIRECTORIES, FILES_AND_DIRECTORIES
	}

	public final static class Builder {
		
		private final static FileManagerOptions DEFAULT = new Builder().build();
		
		private Set<Action> actions = new LinkedHashSet<>();
		private Set<Component> components = new LinkedHashSet<>();
		private Set<NavAction> navActions = new LinkedHashSet<>();
		private Mode mode = Mode.FILES_AND_DIRECTORIES;
		private SelectionMode selectionMode = SelectionMode.SINGLE;
		private boolean fallback;
		private boolean rowActions;
		
		public Builder fromAttributeValue(String val) {
			var parts = Lang.splitStringList(val, ";");
			withActions(Lang.splitStringList(parts.get(0), ",").stream().map(Action::valueOf).toList());
			withComponents(Lang.splitStringList(parts.get(1), ",").stream().map(Component::valueOf).toList());
			withNavActions(Lang.splitStringList(parts.get(2), ",").stream().map(NavAction::valueOf).toList());
			withMode(Mode.valueOf(parts.get(3)));
			withSelectionMode(SelectionMode.valueOf(parts.get(4)));
			withFallback("true".equals(parts.get(5)));
			withRowActions("true".equals(parts.get(6)));
			return this;
		}

		public Builder withFallback() {
			return withFallback(true);
		}
		
		public Builder withFallback(boolean fallback) {
			this.fallback = fallback;
			return this;
		}

		public Builder withRowActions() {
			return withRowActions(true);
		}
		
		public Builder withRowActions(boolean properties) {
			this.rowActions = properties;
			return this;
		}
		
		public Builder withMode(Mode mode) {
			this.mode = mode;
			return this;
		}
		
		public Builder withSelectionMode(SelectionMode selectionMode) {
			this.selectionMode = selectionMode;
			return this;
		}

		public Builder withNavActions(NavAction... navActions) {
			return withNavActions(Arrays.asList(navActions));
		}
		
		public Builder withNavActions(Collection<NavAction> navActions) {
			this.navActions.clear();
			this.navActions.addAll(navActions);
			return this;
		}

		public Builder withActions(Action... actions) {
			return withActions(Arrays.asList(actions));
		}
		
		public Builder withActions(Collection<Action> actions) {
			this.actions.clear();
			this.actions.addAll(actions);
			return this;
		}

		public Builder withComponents(Component... actions) {
			return withComponents(Arrays.asList(actions));
		}
		
		public Builder withComponents(Collection<Component> actions) {
			this.components.clear();
			this.components.addAll(actions);
			return this;
		}
		
		public FileManagerOptions build() {
			return new FileManagerOptions(this);
		}
	}
	
	public static FileManagerOptions defaultOptions() {
		return forBrowser(); 
	}
	
	public static FileManagerOptions fromAttributeValue(String value) {
		return new Builder().fromAttributeValue(value).build();
	}

	public static FileManagerOptions forChooser() {
		return forChooser(Mode.FILES_AND_DIRECTORIES);
	}
	
	public static FileManagerOptions forChooser(Mode mode) {
		return new FileManagerOptions.Builder().
			withFallback().
			withMode(mode).
			withComponents(Component.LOCATION_BAR, Component.NAVIGATION, Component.OPTIONS).
			withNavActions(NavAction.forLocationBar()).
			build();
	}
	
	public static FileManagerOptions forBrowser() {
		return new FileManagerOptions.Builder().
			withSelectionMode(SelectionMode.MULTIPLE).
			withComponents(Component.TOOLBAR, Component.NAVIGATION, Component.SEARCH, Component.OPTIONS).
			withNavActions(NavAction.forBrowser()).
			withRowActions().
			build();
	}
	
	private final Set<Action> actions;
	private final Set<NavAction> navActions;
	private final Set<Component> components;
	private final Mode mode;
	private final SelectionMode selectionMode;
	private final boolean fallback;
	private final boolean rowActions;

	private FileManagerOptions(Builder builder) {
		this.actions = Collections.unmodifiableSet(new LinkedHashSet<>(builder.actions));
		this.navActions = Collections.unmodifiableSet(new LinkedHashSet<>(builder.navActions));
		this.components = Collections.unmodifiableSet(new LinkedHashSet<>(builder.components));
		this.mode = builder.mode;
		this.selectionMode = builder.selectionMode;
		this.fallback = builder.fallback;
		this.rowActions = builder.rowActions;
	}
	
	public boolean fallback() {
		return fallback;
	}

	public boolean rowActions() {
		return rowActions;
	}
	
	public Mode mode() {
		return mode;
	}
	
	public SelectionMode selectionMode() {
		return selectionMode;
	}
	
	public Set<Action> actions() {
		return actions;
	}
	
	public Set<NavAction> navActions() {
		return navActions;
	}
	
	public Set<Component> components() {
		return components;
	}
	
	public String asAttributeValue() {
		var b = new StringBuilder();
		b.append(String.join(",", actions.stream().map(Action::name).toList()));
		b.append(";");
		b.append(String.join(",", components.stream().map(Component::name).toList()));
		b.append(";");
		b.append(String.join(",", navActions.stream().map(NavAction::name).toList()));
		b.append(";");
		b.append(mode.name());
		b.append(";");
		b.append(selectionMode.name());
		b.append(";");
		b.append(fallback);
		b.append(";");
		b.append(rowActions);
		return b.toString();
	}
}
