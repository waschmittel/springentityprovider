package com.bfits.util.vaadin.spring;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bfits.util.vaadin.spring.SpringEntityProvider.ExampleFilter;
import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.shared.data.sort.SortDirection;

/**
 * Implement this so that Spring can properly autoinject.
 *
 * <p>
 * Example:
 * </p>
 *
 * <pre>
 * {@code
 * public interface EmployeeRepository extends JpaRepository<Employee, Serializable> {
 *     {@literal@}Repository
 *     public class EmployeeProvider extends SpringEntityProvider<Employee> {
 *     }
 * }
 * }
 * </pre>
 *
 * You can then use
 *
 * <pre>
 * {@code
 * {@literal@}Autoinject
 * EmployeeProvider employeeProvider;
 *
 * ...
 *
 * myGrid.setDataProvider(employeeProvider);
 * }
 * </pre>
 *
 * @author FlassakD
 *
 * @param <T> Entity for which to provide data
 */
public abstract class SpringEntityProvider<T> extends AbstractBackEndDataProvider<T, ExampleFilter<T>> {

    /**
     * filter to filter by example
     *
     * @author FlassakD
     *
     * @param <T> type of filtered entity
     */
    public static final class ExampleFilter<T> {
        private final Example<T> example;

        /**
         * create an example filter with the default matcher (ignore case and match strings with CONTAINING)
         *
         * @param probe Probe to filter by
         */
        public ExampleFilter(T probe) {
            this(probe, ExampleMatcher.matching().withIgnoreCase().withStringMatcher(StringMatcher.CONTAINING));
        }
        // TODO: find out what the difference is between matching() matchingAll etc...

        /**
         * create an example filter with a custom matcher
         *
         * @param probe Probe to filter by
         * @param exampleMatcher example matcher to use
         */
        public ExampleFilter(T probe, ExampleMatcher exampleMatcher) {
            example = Example.of(probe, exampleMatcher);
        }
    }

    @Autowired
    private JpaRepository<T, Serializable> repository;

    private int resultLimit = 0;

    public SpringEntityProvider() {
    }

    @Override
    protected Stream<T> fetchFromBackEnd(Query<T, ExampleFilter<T>> query) {
        int page = query.getOffset() / query.getLimit();

        List<Sort.Order> sortOrders = query.getSortOrders().stream()
                                           .map(sortOrder -> new Sort.Order(sortOrder.getDirection() == SortDirection.ASCENDING ? Sort.Direction.ASC
                                                                                                                                : Sort.Direction.DESC,
                                                                            sortOrder.getSorted()))
                                           .collect(Collectors.toList());

        PageRequest pageRequest = new PageRequest(page, query.getLimit(), sortOrders.isEmpty() ? null : new Sort(sortOrders));
        List<T> items = null;
        if (query.getFilter().isPresent()) {
            items = repository.findAll(query.getFilter().get().example, pageRequest).getContent();
        }
        else {
            items = repository.findAll(pageRequest).getContent();
        }
        return items.subList(query.getOffset() % query.getLimit(), items.size()).stream(); // TODO: comment why this is done
    }

    @Override
    protected int sizeInBackEnd(Query<T, ExampleFilter<T>> query) {
        int size = 0;
        if (query.getFilter().isPresent()) {
            size = Math.toIntExact(repository.count(query.getFilter().get().example));
        }
        else {
            size = Math.toIntExact(repository.count());
        }
        if (resultLimit > 0 && size > resultLimit) {
            return resultLimit;
        }
        return size;
    }

    /**
     * limit the maximum number of returned entities (limit the maximum numbers of rows if this is used in a Grid)
     *
     * @param resultLimit maximum number of entities
     */
    public void setLimit(int resultLimit) {
        if (resultLimit <= 0) {
            throw new InvalidParameterException("maxSize must be positive");
        }
        this.resultLimit = resultLimit;
    }

    /**
     * use this to filter by example
     *
     * <pre>
     * {@code
     * Employee filterEmployee = new Employee();
     * filterEmployee<br>.setName("John Doe");
     * employeeGrid.setDataProvider(employeeProvider.withFilter(new ExampleFilter<>(filterEmployee)));
     * }
     * </pre>
     *
     * @param exampleFilter example filter, see {@link ExampleFilter}
     * @return a filtered data provider
     *
     */
    public ConfigurableFilterDataProvider<T, Void, ExampleFilter<T>> withFilter(ExampleFilter<T> exampleFilter) {
        ConfigurableFilterDataProvider<T, Void, ExampleFilter<T>> filteredProvider = withConfigurableFilter();
        filteredProvider.setFilter(exampleFilter);
        return filteredProvider;
    }

}
