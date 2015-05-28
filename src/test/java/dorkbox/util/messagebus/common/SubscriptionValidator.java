package dorkbox.util.messagebus.common;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dorkbox.util.messagebus.SubscriptionManager;
import dorkbox.util.messagebus.subscription.Subscription;

/**
*
* @author bennidi
*         Date: 5/25/13
*/
public class SubscriptionValidator extends AssertSupport{


    private List<ValidationEntry> validations = new LinkedList<ValidationEntry>();
    private Set<Class> messageTypes = new HashSet<Class>();
    private ListenerFactory subscribedListener; // the subscribed listeners are used to assert the size of the subscriptions

    public SubscriptionValidator(ListenerFactory subscribedListener) {
        this.subscribedListener = subscribedListener;
    }

    public Expectation listener(Class subscriber){
        return new Expectation(subscriber);
    }

    private SubscriptionValidator expect(Class subscriber, Class messageType){
        this.validations.add(new ValidationEntry(messageType, subscriber));
        this.messageTypes.add(messageType);
        return this;
    }

    // match subscriptions with existing validation entries
    // for each tuple of subscriber and message type the specified number of listeners must exist
    public void validate(SubscriptionManager manager){
        for (Class messageType : this.messageTypes){
            Collection<ValidationEntry> validationEntries = getEntries(messageType);

            // we split subs + superSubs into TWO calls.
            Collection<Subscription> collection = new ArrayDeque<Subscription>(8);
            Subscription[] subscriptions = manager.getSubscriptionsByMessageType(messageType);
            if (subscriptions != null) {
                collection.addAll(Arrays.asList(subscriptions));
            }
            Subscription[] superSubs = manager.getSuperSubscriptions(messageType);
            if (superSubs != null) {
                collection.addAll(Arrays.asList(superSubs));
            }

            assertEquals(validationEntries.size(), collection.size());


            for(ValidationEntry validationValidationEntry : validationEntries){
                Subscription matchingSub = null;
                // one of the subscriptions must belong to the subscriber type
                for(Subscription sub : collection){
                    if(sub.belongsTo(validationValidationEntry.subscriber)){
                        matchingSub = sub;
                        break;
                    }
                }
                assertNotNull(matchingSub);
                assertEquals(this.subscribedListener.getNumberOfListeners(validationValidationEntry.subscriber), matchingSub.size());
            }
        }
    }


    private Collection<ValidationEntry> getEntries(Class<?> messageType){
        Collection<ValidationEntry> matching = new LinkedList<ValidationEntry>();
        for (ValidationEntry validationValidationEntry : this.validations){

            if(validationValidationEntry.messageType.equals(messageType)) {
                matching.add(validationValidationEntry);
            }
        }
        return matching;
    }




    public class Expectation{

        private Class listener;

        private Expectation(Class listener) {
            this.listener = listener;
        }

        public SubscriptionValidator handles(Class ...messages){
            for(Class message : messages) {
                expect(this.listener, message);
            }
            return SubscriptionValidator.this;
        }
    }

    private class ValidationEntry {


        private Class subscriber;

        private Class messageType;

        private ValidationEntry(Class messageType, Class subscriber) {
            this.messageType = messageType;
            this.subscriber = subscriber;
        }


    }

}
