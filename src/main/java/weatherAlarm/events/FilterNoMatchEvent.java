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

package weatherAlarm.events;

import weatherAlarm.model.WeatherAlarm;

/**
 * @author <a href="mailto:john.scattergood@gmail.com">John Scattergood</a> 1/4/2015
 */
public class FilterNoMatchEvent implements IModuleEvent {
    private Reason reason;
    private WeatherAlarm alarm;

    public FilterNoMatchEvent(Reason reason, WeatherAlarm alarm) {
        this.reason = reason;
        this.alarm = alarm;
    }

    public Reason getReason() {
        return reason;
    }

    public WeatherAlarm getAlarm() {
        return alarm;
    }

    @Override
    public String toString() {
        return "FilterNoMatchEvent[" +
                "reason=" + reason +
                ", alarm=" + alarm +
                ']';
    }

    public enum Reason {
        CRITERIA,
        NOT_READY
    }
}
