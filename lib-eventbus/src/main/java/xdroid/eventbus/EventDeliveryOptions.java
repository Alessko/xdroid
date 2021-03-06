package xdroid.eventbus;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

import java.lang.reflect.Method;
import java.util.Map;

import xdroid.core.FragmentManagerHelper;

import static xdroid.collections.Prototypes.newHashMap;
import static xdroid.core.ObjectUtils.notNull;
import static xdroid.eventbus.BuildConfig.SNAPSHOT;

/**
 * @author Oleksii Kropachov (o.kropachov@shamanland.com)
 */
public class EventDeliveryOptions implements Parcelable {
    public static final int APPEARANCE_REPLACE = 0;
    public static final int APPEARANCE_ADD = 1;

    public static final int TRANSITION_NONE = 0;
    public static final int TRANSITION_OPEN = 1;
    public static final int TRANSITION_CLOSE = 2;
    public static final int TRANSITION_FADE = 3;

    // NOTE mandatory fields
    public final String fragment;
    public final String tag;
    public final int container;

    // NOTE attaching rules
    public final int appearance;
    public final boolean backStack;
    public final String backStackState;
    public final boolean obeyOnForceBackStackEvenForSingleEntry;
    public final boolean popOnForceBackStackEvenForSingleEntry;

    // NOTE customisation
    public final int breadCrumbShortTitle;
    public final int breadCrumbTitle;
    public final int enterAnimation;
    public final int exitAnimation;
    public final int popEnterAnimation;
    public final int popExitAnimation;
    public final int transition;
    public final int transitionStyle;

    // NOTE helpers
    public final boolean customAnimations;

    public EventDeliveryOptions(Context context, AttributeSet attrs) {
        TypedArray a = notNull(context.obtainStyledAttributes(attrs, R.styleable.EventDelivery));

        try {
            fragment = EventDispatcherInflater.readClass(context, a, R.styleable.EventDelivery_fragment);

            tag = a.getString(R.styleable.EventDelivery_tag);
            if (tag == null) {
                if (SNAPSHOT) {
                    throw new IllegalArgumentException();
                }
            }

            container = a.getResourceId(R.styleable.EventDelivery_container, 0);
            if (container == 0) {
                if (SNAPSHOT) {
                    throw new IllegalArgumentException();
                }
            }

            appearance = a.getInteger(R.styleable.EventDelivery_appearance, APPEARANCE_REPLACE);
            backStack = a.getBoolean(R.styleable.EventDelivery_back_stack, false);
            backStackState = a.getString(R.styleable.EventDelivery_back_stack_state);
            obeyOnForceBackStackEvenForSingleEntry = a.getBoolean(R.styleable.EventDelivery_obey_on_force_back_stack_even_for_single_entry, false);
            popOnForceBackStackEvenForSingleEntry = a.getBoolean(R.styleable.EventDelivery_pop_on_force_back_stack_even_for_single_entry, true);

            breadCrumbShortTitle = a.getResourceId(R.styleable.EventDelivery_bread_crumb_short_title, 0);
            breadCrumbTitle = a.getResourceId(R.styleable.EventDelivery_bread_crumb_title, 0);
            enterAnimation = a.getResourceId(R.styleable.EventDelivery_enter_animation, 0);
            exitAnimation = a.getResourceId(R.styleable.EventDelivery_exit_animation, 0);
            popEnterAnimation = a.getResourceId(R.styleable.EventDelivery_pop_enter_animation, 0);
            popExitAnimation = a.getResourceId(R.styleable.EventDelivery_pop_exit_animation, 0);
            transition = a.getInt(R.styleable.EventDelivery_transition, -1);
            transitionStyle = a.getResourceId(R.styleable.EventDelivery_transition_style, 0);
        } finally {
            a.recycle();
        }

        customAnimations = useCustomAnimations();
    }

    public void performTransaction(FragmentManagerHelper manager, Object fragment) {
        Object transaction = manager.beginTransaction();

        switch (transition) {
            case TRANSITION_NONE:
            case TRANSITION_OPEN:
            case TRANSITION_CLOSE:
            case TRANSITION_FADE:
                manager.transactionSetTransition(transaction, transition);
                break;
        }

        if (transitionStyle != 0) {
            manager.transactionSetTransitionStyle(transaction, transitionStyle);
        }

        if (customAnimations) {
            manager.transactionSetCustomAnimations(transaction, enterAnimation, exitAnimation, popEnterAnimation, popExitAnimation);
        }

        switch (appearance) {
            case APPEARANCE_REPLACE:
                manager.transactionReplace(transaction, container, fragment, tag);
                break;

            case APPEARANCE_ADD:
                manager.transactionAdd(transaction, container, fragment, tag);
                break;
        }

        boolean forcedBackStack = false;

        if (!backStack && appearance == APPEARANCE_REPLACE) {
            Object old = manager.findFragmentById(container);
            if (old != null) {
                if (MethodIsInBackStack.invoke(old)) {
                    if (manager.getBackStackEntryCount() == 1) {
                        if (popOnForceBackStackEvenForSingleEntry) {
                            manager.popBackStack();
                        }

                        if (obeyOnForceBackStackEvenForSingleEntry) {
                            forcedBackStack = true;
                        }
                    } else {
                        forcedBackStack = true;
                    }
                }
            }
        }

        if (backStack || forcedBackStack) {
            manager.transactionAddToBackStack(transaction, backStackState);
        }

        if (breadCrumbShortTitle != 0) {
            manager.transactionSetBreadCrumbShortTitle(transaction, breadCrumbShortTitle);
        }

        if (breadCrumbTitle != 0) {
            manager.transactionSetBreadCrumbTitle(transaction, breadCrumbTitle);
        }

        manager.transactionCommit(transaction);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(fragment);
        out.writeString(tag);
        out.writeInt(container);

        out.writeInt(appearance);
        int bs = backStack ? 1 : 0;
        if (backStackState != null) {
            out.writeInt(bs | 2);
            out.writeString(backStackState);
        } else {
            out.writeInt(bs);
        }
        out.writeInt(obeyOnForceBackStackEvenForSingleEntry ? 1 : 0);
        out.writeInt(popOnForceBackStackEvenForSingleEntry ? 1 : 0);

        out.writeInt(breadCrumbShortTitle);
        out.writeInt(breadCrumbTitle);
        out.writeInt(enterAnimation);
        out.writeInt(exitAnimation);
        out.writeInt(popEnterAnimation);
        out.writeInt(popExitAnimation);
        out.writeInt(transition);
        out.writeInt(transitionStyle);
    }

    public static final Creator<EventDeliveryOptions> CREATOR = new Creator<EventDeliveryOptions>() {
        public EventDeliveryOptions createFromParcel(Parcel in) {
            return new EventDeliveryOptions(in);
        }

        public EventDeliveryOptions[] newArray(int size) {
            return new EventDeliveryOptions[size];
        }
    };

    private EventDeliveryOptions(Parcel in) {
        fragment = in.readString();
        tag = in.readString();
        container = in.readInt();

        appearance = in.readInt();
        int bs = in.readInt();
        backStack = (bs & 1) != 0;
        backStackState = (bs & 2) != 0 ? in.readString() : null;
        obeyOnForceBackStackEvenForSingleEntry = in.readInt() == 1;
        popOnForceBackStackEvenForSingleEntry = in.readInt() == 1;

        breadCrumbShortTitle = in.readInt();
        breadCrumbTitle = in.readInt();
        enterAnimation = in.readInt();
        exitAnimation = in.readInt();
        popEnterAnimation = in.readInt();
        popExitAnimation = in.readInt();
        transition = in.readInt();
        transitionStyle = in.readInt();

        customAnimations = useCustomAnimations();
    }

    private boolean useCustomAnimations() {
        return (enterAnimation | exitAnimation | popEnterAnimation | popExitAnimation) != 0;
    }

    static class MethodIsInBackStack {
        private static final Map<String, Method> sMethods = newHashMap();

        static Method getMethod(Class clazz) {
            try {
                //noinspection unchecked
                Method result = clazz.getDeclaredMethod("isInBackStack");
                result.setAccessible(true);
                return result;
            } catch (Throwable ex) {
                clazz = clazz.getSuperclass();
                if (clazz != null) {
                    return getMethod(clazz);
                } else if (SNAPSHOT) {
                    throw new AssertionError(ex);
                }

                return null;
            }
        }

        static boolean invoke(Object fragment) {
            Method m = sMethods.get(fragment.getClass().getName());
            if (m == null) {
                m = getMethod(fragment.getClass());
                if (m == null) {
                    return false;
                }

                sMethods.put(fragment.getClass().getName(), m);
            }

            Object result;

            try {
                result = m.invoke(fragment);
            } catch (Throwable ex) {
                if (SNAPSHOT) {
                    throw new AssertionError(ex);
                }

                return false;
            }

            if (result instanceof Boolean) {
                return (Boolean) result;
            }

            return false;
        }
    }
}
