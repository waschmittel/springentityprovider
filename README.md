# Spring Entity Provider for Vaadin

This is a data provider for Vaadin 8 using Spring Data. It supports pagination and sorting. Avoids boilerplate code.

# Example

You can find a complete [example project here](https://github.com/waschmittel/springentityprovider-demo).

## Entity

```java
@Entity
@Access(AccessType.FIELD)
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(message = "Name is required.")
    @Column(length = 50)
    private String name;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }
}
```

## Repository and SpringEntityProvider implementation class

```java
public interface PersonRepository extends JpaRepository<Person, Serializable> {
    @Repository
    public class PersonProvider extends SpringEntityProvider<Person> {
    }
}
```

## UI

```java
@SpringUI
public class MySpringUI extends UI {
    @Autowired
    PersonProvider personProvider;
    
    @Override
    protected void init(VaadinRequest request) 
        Grid<Person> personGrid = new Grid<>(Person.class);
        personGrid.setDataProvider(personProvider);
        
        ...
    }
}
```

## Filtering

Show only persons whose name contains "John":
```java
Person filterPerson = new Person();
filterPerson.setName("John");
ExampleFilter<Person> personFilter = new ExampleFilter<>(filterPerson);
personGrid.setDataProvider(personProvider.withFilter(personFilter));
```

## Limit

Limit the maximum rows of the Grid:
```java
personProvider.setLimit(1000);
personGrid.setDataProvider(personProvider);
```
