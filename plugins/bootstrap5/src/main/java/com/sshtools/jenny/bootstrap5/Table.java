/**
 * Copyright © 2023 JAdaptive Limited (support@jadaptive.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.jenny.bootstrap5;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.sshtools.tinytemplate.Templates.TemplateModel;

public interface Table {
	
	public interface Cell {
		TemplateModel template(TemplateModel prototype);
		
		public static Cell ofText(String text) {
			return (templ) -> templ.variable("text", text);
		}
		
		public static Cell ofText(Supplier<String> text) {
			return (templ) -> templ.variable("text", text);
		}
	}
	
	@FunctionalInterface
	public interface RowMapper<ROW> {
		Collection<Cell> map(TemplateModel prototype, ROW row);
	}
	
	@FunctionalInterface
	public interface RowModel<ROW> {
		ROW row(int index);
	}
	
	public static class Builder<ROW> {
		
		private Set<Supplier<Cell>> columns = new LinkedHashSet<>();
		private RowModel<ROW> rowModel;
		private RowMapper<ROW> rowMapper;
		
		public Builder<ROW> withColumnText(String... columns) {
			return withColumnText(columns);
		}
		public Builder<ROW> withColumnText(Collection<String> columns) {
			return withColumnNames(columns.stream().map(c -> new Supplier<String>() {
				@Override
				public String get() {
					return c;
				}
			}).toList());
		}
		
		public Builder<ROW> withColumnNames(Collection<? extends Supplier<String>> columns) {
			return withColumns(columns.stream().map(c -> new Supplier<Cell>() {
				@Override
				public Cell get() {
					return new DefaultCellImpl(c.get());
				}
			}).toList());
		}
		
		public Builder<ROW> withColumnNames(@SuppressWarnings("unchecked") Supplier<String>... columns) {
			return withColumnNames(Arrays.asList(columns));
		}
		public Builder<ROW> withColumns(Collection<? extends Supplier<Cell>> columns) {
			this.columns.addAll(columns);
			return this;
		}
		
		public Builder<ROW> withColumns(@SuppressWarnings("unchecked") Supplier<Cell>... columns) {
			return withColumns(Arrays.asList(columns));
		}
		
		public Builder<ROW>  fromList(List<ROW> rows, RowMapper<ROW> mapper) {
			withRowModel((idx) -> {
				if(idx == rows.size())
					return null;
				return rows.get(idx);
			}, mapper);
			return this;
		}
		
		public Builder<ROW>  withRowModel(RowModel<ROW> rowModel, RowMapper<ROW> rowMapper) {
			this.rowModel = rowModel;
			this.rowMapper = rowMapper;
			return this;
		}
		
		public Table build() {
			if(rowModel == null)
				throw new IllegalStateException("No row model set.");
			if(rowMapper == null)
				throw new IllegalStateException("No row mapper set.");
			return new DefaultTableImpl<ROW>(rowModel, rowMapper);
		}
		
		private final static class DefaultCellImpl implements Cell {
			
			private final String text;

			DefaultCellImpl(String text) {
				this.text = text;
			}

			@Override
			public TemplateModel template(TemplateModel prototype) {
				prototype.variable("text", () -> text);
				return prototype;
			}
			
		}
		
		private final static class DefaultTableImpl<ROW> implements Table {

			private final RowModel<ROW> rowModel;
			private final RowMapper<ROW> rowMapper;

			DefaultTableImpl(RowModel<ROW> rowModel, RowMapper<ROW> rowMapper) {
				this.rowModel = rowModel;
				this.rowMapper = rowMapper;
			}

			@Override
			public TemplateModel template(TemplateModel template) {
				template.list("rows", (content) -> rows().map(r -> {
					var rowTemplate = TemplateModel.ofContent(content);
					
					var columns = rowMapper.map(rowTemplate, r);
					rowTemplate.list("columns", (colContent) -> 
						columns.stream().map(col -> { 
							return col.template(TemplateModel.ofContent(colContent));
						}).toList()
					);
					
					return rowTemplate;
				}).toList()); 
				return template;
			}
			
			private Stream<ROW> rows() {
				var it = new Iterator<ROW>() {
					
					ROW row = null;
					AtomicInteger idx = new AtomicInteger();

					@Override
					public boolean hasNext() {
						checkNext();
						return row != null;
					}

					@Override
					public ROW next() {
						try {
							checkNext();
							return row;
						}
						finally {
							idx.incrementAndGet();
							row = null;
						}
					}
					
					void checkNext() {
						if(row == null) {
							row = rowModel.row(idx.get());
						}
					}
					
				};
				return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false);
			}
			
		}
		
	}
	
	default TemplateModel template() {
		return template(TemplateModel.ofResource(Table.class, "table.frag.html"));
	}

	TemplateModel template(TemplateModel model);
	
	
}
