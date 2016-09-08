package org.apache.lucene.search;

public class NamedQuery extends Query {

  protected final Query delegate;
  protected final String name;

  public NamedQuery(Query delegate, String name) {
    this.delegate = delegate;
    this.name = name;
  }

  public String getName() { return name; }

  @Override
  public String toString(String field) {
    return name + ": " + delegate.toString(field);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NamedQuery that = (NamedQuery) o;

    return delegate.equals(that.delegate) && name.equals(that.name);

  }

  @Override
  public int hashCode() {
    int result = delegate.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }
}
