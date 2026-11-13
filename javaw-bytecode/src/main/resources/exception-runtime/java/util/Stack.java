package java.util;
public class Stack<E> extends Vector<E> {
 public Stack(){}
 public E push(E item){addElement(item);return item;}
 public E peek(){if(size()==0)throw new EmptyStackException();return elementAt(size()-1);}
 public E pop(){E top=peek();removeElementAt(size()-1);return top;}
 public boolean empty(){return size()==0;}
 public int search(Object o){int i=lastIndexOf(o);return i>=0?size()-i:-1;}
}
