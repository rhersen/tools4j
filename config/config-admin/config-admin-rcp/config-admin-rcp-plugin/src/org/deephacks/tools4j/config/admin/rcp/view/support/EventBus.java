/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.tools4j.config.admin.rcp.view.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.deephacks.tools4j.config.admin.rcp.model.Events.ErrorEvent;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;

/**
 * Helper class for publishing and listening for events between views in a 
 * type-safe and less verbose way. 
 * 
 * This mechanism is built on ISourceProvider and ISourceProviderListener. 
 * 
 */
public class EventBus extends AbstractSourceProvider {
    private static final String VARIABLE = "EventBus";

    public static <T> void fire(T event) {
        getSourceProvider().internalFire(event);
    }

    public static <T> void registerListener(final Object listener) {
        Method[] methods = listener.getClass().getDeclaredMethods();
        final HashMap<Class<?>, Method> listenerMethods = findListenerMethods(methods);
        if (listenerMethods.isEmpty()) {
            throw new IllegalArgumentException("Listener [" + listener
                    + "] does not observe any events, use @Observe on method parameters.");
        }
        for (final Class<?> eventClazz : listenerMethods.keySet()) {
            ISourceProviderListener sourceProviderListener = new ISourceProviderListener() {
                Object delegateListener = listener;
                Class<?> eventClass = eventClazz;
                Method listenerMethod = listenerMethods.get(eventClazz);

                @SuppressWarnings("rawtypes")
                public void sourceChanged(int sourcePriority, Map sourceValuesByName) {
                    Object event = sourceValuesByName.get(eventClass.getName());
                    if (event == null) {
                        // no subscription for event
                        return;
                    }
                    try {
                        listenerMethod.invoke(delegateListener, event);
                    } catch (InvocationTargetException e) {
                        // not much to do, should we throw exception or not?
                        EventBus.fire(new ErrorEvent(e.getCause()));

                    } catch (Exception e) {
                        // not much to do, should we throw exception or not?
                        EventBus.fire(new ErrorEvent(e));
                    }
                }

                public void sourceChanged(int sourcePriority, String sourceName, Object sourceValue) {

                    throw new IllegalArgumentException(
                            "Not implemented yet, in what cases is it even needed?");
                }
            };
            getSourceProvider().addSourceProviderListener(sourceProviderListener);
        }
    }

    private static HashMap<Class<?>, Method> findListenerMethods(Method[] methods) {
        final HashMap<Class<?>, Method> listenerMethods = new HashMap<Class<?>, Method>();
        for (Method method : methods) {
            method.setAccessible(true);
            Class<?>[] parameterTypes = method.getParameterTypes();
            Annotation[][] annos = method.getParameterAnnotations();
            int i = 0;
            for (Annotation[] annotations : annos) {
                Class<?> type = parameterTypes[i++];
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Observes) {
                        listenerMethods.put(type, method);
                    }
                }
            }
        }
        return listenerMethods;
    }

    private static EventBus getSourceProvider() {
        IWorkbenchWindow w = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        ISourceProviderService service = (ISourceProviderService) w
                .getService(ISourceProviderService.class);
        return (EventBus) service.getSourceProvider(VARIABLE);
    }

    public static interface BusEventListener<T> {
        public void event(T event);
    }

    public void dispose() {
        // nothing to do 
    }

    @SuppressWarnings("rawtypes")
    public Map getCurrentState() {
        return new HashMap();
    }

    public String[] getProvidedSourceNames() {
        return new String[] { VARIABLE };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    <T> void internalFire(T event) {
        HashMap map = new HashMap();
        map.put(event.getClass().getName(), event);
        fireSourceChanged(1, map);
    }

}
