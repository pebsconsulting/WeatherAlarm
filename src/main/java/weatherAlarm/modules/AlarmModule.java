/*
 * Copyright 2015 John Scattergood
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

package weatherAlarm.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;
import weatherAlarm.events.*;
import weatherAlarm.model.WeatherAlarm;
import weatherAlarm.model.WeatherConditions;
import weatherAlarm.model.WeatherDataEnum;
import weatherAlarm.util.PredicateEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is responsible for filtering current weather conditions to determining if an alarm should be raised
 *
 * @author <a href="mailto:john.scattergood@gmail.com">John Scattergood</a> 12/30/2014
 */
public class AlarmModule extends EventModule {
    private static final Logger logger = LoggerFactory.getLogger(AlarmModule.class);
    private List<WeatherAlarm> alarms = new ArrayList<>();

    public AlarmModule(EventStream stream) {
        super(stream);
    }

    @Override
    protected void configure() {
        addAlarms();

        final Observable<IModuleEvent> observableFilterEvent = eventStream
                .observe(WeatherConditionEvent.class)
                .flatMap(evaluateEvent());
        eventStream.publish(observableFilterEvent);

        final Observable<IModuleEvent> observableNotificationEvent = eventStream
                .observe(NotificationSentEvent.class)
                .flatMap(handleNotification());
        eventStream.publish(observableNotificationEvent);
    }

    private void addAlarms() {
        final String userName = System.getProperty("weatherAlarm.userName");
        if (userName == null) {
            logger.error("No user defined. Not adding alarm...");
            return;
        }
        final String userEmail = System.getProperty("weatherAlarm.userEmail");
        if (userEmail == null) {
            logger.error("No user email defined. Not adding alarm...");
            return;
        }
        final String temperaturePredicate = System.getProperty("weatherAlarm.temperaturePredicate");
        final PredicateEnum predicateEnum = PredicateEnum.valueOf(temperaturePredicate);
        if (predicateEnum == null) {
            logger.error("Invalid predicate enum " + temperaturePredicate + ". Not adding alarm...");
            return;
        }
        final String temperatureValue = System.getProperty("weatherAlarm.temperatureValue");
        final Integer value;
        try {
            value = Integer.parseInt(temperatureValue);
        } catch (NumberFormatException e) {
            logger.error("Invalid temperature value " + temperatureValue + ". Not adding alarm...");
            return;
        }

        WeatherAlarm alarm = new WeatherAlarm(userName, userEmail);
        WeatherAlarm.ValuePredicate<Integer> predicate = new WeatherAlarm.ValuePredicate<>(predicateEnum, value);
        alarm.setCriteria(WeatherDataEnum.TEMPERATURE, predicate);
        alarms.add(alarm);
    }

    private Func1<? super WeatherConditionEvent, ? extends Observable<IModuleEvent>> evaluateEvent() {
        return event -> {
            WeatherConditions conditions = event.getConditions();
            List<WeatherAlarm> matchingAlarms = alarms.stream()
                    .filter(alarm -> alarm.matchesCriteria(WeatherDataEnum.TEMPERATURE, conditions.getTemperature())
                            && alarm.shouldSendNotification())
                    .collect(Collectors.toList());
            if (matchingAlarms.size() > 0) {
                List<IModuleEvent> events = matchingAlarms.stream()
                        .flatMap(alarm -> Stream.of(new FilterMatchEvent(alarm, conditions)))
                        .collect(Collectors.toList());
                return Observable.from(events);
            } else {
                return Observable.just(new FilterNoMatchEvent());
            }
        };
    }

    private Func1<? super NotificationSentEvent, ? extends Observable<IModuleEvent>> handleNotification() {
        return event -> {
            WeatherAlarm alarm = event.getAlarm();
            alarm.setLastNotification(event.getEventTime());
            return Observable.just(new WeatherAlarmUpdatedEvent(alarm));
        };
    }
}
