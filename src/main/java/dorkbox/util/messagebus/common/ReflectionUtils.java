package dorkbox.util.messagebus.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import dorkbox.util.messagebus.annotations.Handler;
import dorkbox.util.messagebus.common.thread.ConcurrentSet;

/**
 * @author bennidi
 *         Date: 2/16/12
 *         Time: 12:14 PM
 * @author dorkbox
 *         Date: 2/2/15
 */
public class ReflectionUtils {

    public static Collection<Method> getMethods(Class<?> target) {
        Collection<Method> hashSet = new ConcurrentSet<Method>(16, .8F, 1);
        getMethods(target, hashSet);
        return hashSet;
    }

    private static void getMethods(Class<?> target, Collection<Method> methods) {
        try {
            for (Method method : target.getDeclaredMethods()) {
                if (getAnnotation(method, Handler.class) != null) {
                    methods.add(method);
                }
            }
        } catch (Exception ignored) {
        }

        if (!target.equals(Object.class)) {
            getMethods(target.getSuperclass(), methods);
        }
    }

    /**
    * Traverses the class hierarchy upwards, starting at the given subclass, looking
    * for an override of the given methods -> finds the bottom most override of the given
    * method if any exists
    *
    * @param overridingMethod
    * @param subclass
    * @return
    */
    public static Method getOverridingMethod( final Method overridingMethod, final Class<?> subclass ) {
        Class<?> current = subclass;
        while ( !current.equals( overridingMethod.getDeclaringClass() ) ) {
            try {
                return current.getDeclaredMethod( overridingMethod.getName(), overridingMethod.getParameterTypes() );
            }
            catch ( NoSuchMethodException e ) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Collect all directly and indirectly related super types (classes and interfaces) of
     * a given class.
     *
     * @param from The root class to start with
     * @return A set of classes, each representing a super type of the root class
     */
    public static Collection<Class<?>> getSuperTypes(Class<?> from) {
        Collection<Class<?>> superclasses = new ConcurrentSet<Class<?>>(8, 0.8F, 1);

        collectInterfaces( from, superclasses );

        while ( !from.equals( Object.class ) && !from.isInterface() ) {
            superclasses.add( from.getSuperclass() );
            from = from.getSuperclass();
            collectInterfaces( from, superclasses );
        }
        return superclasses;
    }

    public static void collectInterfaces( Class<?> from, Collection<Class<?>> accumulator ) {
        for ( Class<?> intface : from.getInterfaces() ) {
            accumulator.add( intface );
            collectInterfaces( intface, accumulator );
        }
    }

    public static boolean containsOverridingMethod(final Collection<Method> allMethods, final Method methodToCheck) {
        Iterator<Method> iterator;
        Method method;

        for (iterator = allMethods.iterator(); iterator.hasNext();) {
            method = iterator.next();

            if (isOverriddenBy(methodToCheck, method)) {
                return true;
            }
        }
        return false;
    }



    /**
    * Searches for an Annotation of the given type on the class.  Supports meta annotations.
    *
    * @param from AnnotatedElement (class, method...)
    * @param annotationType Annotation class to look for.
    * @param <A> Class of annotation type
    * @return Annotation instance or null
    */
    private static <A extends Annotation> A getAnnotation(AnnotatedElement from, Class<A> annotationType, Collection<AnnotatedElement> visited) {
        if( visited.contains(from) ) {
            return null;
        }
        visited.add(from);
        A ann = from.getAnnotation( annotationType );
        if( ann != null) {
            return ann;
        }
        for ( Annotation metaAnn : from.getAnnotations() ) {
            ann = getAnnotation(metaAnn.annotationType(), annotationType, visited);
            if ( ann != null ) {
                return ann;
            }
        }
        return null;
    }

    public static <A extends Annotation> A getAnnotation( AnnotatedElement from, Class<A> annotationType) {
        A annotation = getAnnotation(from, annotationType, new ConcurrentSet<AnnotatedElement>(16, .8F, 1));
        return annotation;
    }

    //
    private static boolean isOverriddenBy( Method superclassMethod, Method subclassMethod ) {
        // if the declaring classes are the same or the subclass method is not defined in the subclass
        // hierarchy of the given superclass method or the method names are not the same then
        // subclassMethod does not override superclassMethod
        if ( superclassMethod.getDeclaringClass().equals(subclassMethod.getDeclaringClass() )
                || !superclassMethod.getDeclaringClass().isAssignableFrom( subclassMethod.getDeclaringClass() )
                || !superclassMethod.getName().equals(subclassMethod.getName())) {
            return false;
        }

        Class<?>[] superClassMethodParameters = superclassMethod.getParameterTypes();
        Class<?>[] subClassMethodParameters = subclassMethod.getParameterTypes();

        // method must specify the same number of parameters
        //the parameters must occur in the exact same order
        for ( int i = 0; i < subClassMethodParameters.length; i++ ) {
            if ( !superClassMethodParameters[i].equals( subClassMethodParameters[i] ) ) {
                return false;
            }
        }
        return true;
    }

}
