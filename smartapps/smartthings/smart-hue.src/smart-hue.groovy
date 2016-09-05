/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Notify Me With Hue
 *
 *  Author: SmartThings
 *  Date: 2014-01-20
 */
definition(
    name: "Smart Hue",
    namespace: "smartthings",
    author: "Doug Beard",
    description: "Changes the color and brightness of Philips Hue bulbs when any of a variety of SmartThings is activated.  Supports motion, contact, acceleration, moisture and presence sensors as well as switches.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png"
)

preferences {
	page(name: "pageOne", title:"Control What?", nextPage: "pageTwo", uninstall:true){
        	section("Control these bulbs...") {
				//input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
                input "hues", "capability.switchLevel", title: "Which Hue Bulbs?", required:true, multiple:true
            }
            
            section("Choose light effects...")		{
                input "color", "enum", title: "Hue Color?", required: false, multiple:false, options: ["White", "Daylight", "Soft White", "Blue", "Green", "Yellow", "Orange", "Purple", "Pink", "Red"]
                input "lightLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
            }
    }
    page(name: "pageTwo", title: "How?", nextPage: "pageThree", uninstall:false){
        	section("Triggers"){
    			input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
                input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
                input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
                input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
                input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
                input "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
                input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
                input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
                input "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
                input "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
                input "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
                input "triggerModes", "mode", title: "System Changes Mode", description: "Select mode(s)", required: false, multiple: true
                input "timeOfDay", "time", title: "At a Scheduled Time", required: false
        }
        
        section("After?"){
        		input "onMotionStop", "enum", title: "Turn Off when motion stops? (Overrides Duration)", required: false, options: ["Yes","No"]
           		input "motionStopDuration", "number", title: "After how long? (Minutes)", required: false
        }
	}
	page(name: "pageThree", title: "Special Instructions?", nextpage: "pageFour", install:true){
        section ("Sunrise Sunset") {
        	input "sunSetting",  "bool",title: "Use Sun", required: true, defaultValue: false
            input "sunriseOffsetValue", "text", title: "HH:MM", required: false
            input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
            input "sunriseOffsetValue", "text", title: "HH:MM", required: false
            input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
            input "sunsetOffsetValue", "text", title: "HH:MM", required: false
            input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
          	input "zipCode", "text", title: "Zip code", required: false
		}
		section("Frequency (Default Every Message) Duration (Default Infinite)") {
            input "frequency", "decimal", title: "Minutes", required: false
            input "duration", "number", title: "Duration Seconds?", required: false
        }
        section ("Override sunset/sunrise if current percentage level is below... (optional)...") {
            input "currentLevelOverride", "number", title: "Light Level", required: false
        }
         section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
	}
  
}
def installed() {
	log.trace "===========|SmartHue|===========   Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.trace "===========|SmartHue|===========   Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	subscribeToEvents()
}

def subscribeToEvents() {
	log.debug "===========|SmartHue|===========   Subscribe to Events"
	atomicState.isRunning = false
	subscribe(app, appTouchHandler)
	subscribe(contact, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
    subscribe(motion, "motion.inactive", eventHandlerStop)
	subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)
	subscribe(button1, "button.pushed", eventHandler)
    subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)

	if (triggerModes) {
		subscribe(location, modeChangeHandler)
	}

	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}
}

def eventHandler(evt) {
	if (atomicState.isRunning == null || atomicState.isRunning == false){
    	atomicState.isRunning = true
    	log.trace "===========|SmartHue|===========   EventHandler $evt.displayName: $evt.name " 
        log.debug "===========|SmartHue|===========   $evt.data"

        atomicState.trigger = [
                "device": evt.device,
                "time": now()
                ]
        hues.each {
            log.debug "===========|SmartHue|===========  Target Device:  $it"
            if (it.currentValue("level") < currentLevelOverride)  {
                    log.trace "===========|SmartHue|===========   Current Level  -Time Override-"
                    if (frequency) {
                        def lastTime = atomicState[evt.deviceId].lastTime
                        if (lastTime == null || now() - lastTime >= frequency * 60000) {
                            takeAction(evt)
                    }
                }
                else {
                    takeAction(evt)
                }
            }
        }

        log.info "===========|SmartHue|===========   current values = $atomicState.previous"
        if (isTime(evt)){
            if (frequency) {
                def lastTime = atomicState[evt.deviceId]
                if (lastTime == null || now() - lastTime >= frequency * 60000) {
                    takeAction(evt)
                }
            }
            else {
                takeAction(evt)
            }
        } 
        else    {
            if (frequency) {
                def lastTime = atomicState[evt.deviceId]
                if (lastTime == null || now() - lastTime >= frequency * 60000) {
                    takeAction(evt)
                }
            }
            else {
                takeAction(evt)
            }
        }
    }
    else{
    	log.debug "===========|SmartHue|=========== ALREADY RUNNING - todo: figure out how to kick the can"
    }
    
}

def eventHandlerStop(evt){
	log.trace "===========|SmartHue|===========   EventHandlerStop $evt.displayName: $evt.name" 
    log.debug "===========|SmartHue|===========   $evt.data"	
    
    log.debug "===========|SmartHue|===========   $atomicState.previous"
    
    if (onMotionStop == "Yes"){
    	log.info "===========|SmartHue|===========   Motion Stop is On"
        def lastTime = atomicState[evt.deviceId]
        if (lastTime == null || now() - lastTime >= motionStopDuration) {
        	log.info "===========|SmartHue|===========   Setting up Delayed Reset"
            runIn(motionStopDuration, resetHue)
        }
    }
}

def sunriseSunsetTimeHandler(evt) {
	log.trace "===========|SmartHue|===========   sunriseSetTimeHandler $evt.displayName: $evt.name"
    log.debug "===========|SmartHue|===========   $evt.data"
	atomicState.lastSunriseSunsetEvent = now()
	log.info "===========|SmartHue|===========   sunriseSunsetTimeHandler($app.id)"
	astroCheck(evt)
}

def astroCheck(evt) {
	log.trace "===========|SmartHue|===========   atroCheck $evt.displayName: $evt.name"
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	atomicState.riseTime = s.sunrise.time
	atomicState.setTime = s.sunset.time
	log.info "===========|SmartHue|===========   rise: ${new Date(atomicState.riseTime)}($atomicState.riseTime), set: ${new Date(atomicState.setTime)}($atomicState.setTime)"
}

private isTime(evt){
	log.trace "===========|SmartHue|===========   isTime $evt.displayName: $evt.name"
	astroCheck(evt)
	def result
    def t = now()
    log.info "===========|SmartHue|===========   ${new Date(t)}($t)"
    result = t < atomicState.riseTime  || t > atomicState.setTime
    if (!result){
    	log.info "===========|SmartHue|===========    Event is outside the time contraints for operation, skipping ==================" 
    }
    
    if (sunSetting == false){
    	result = true
        }
	result
}

private getSunriseOffset(evt) {
	log.trace "===========|SmartHue|===========   getSunriseOffset $evt.displayName: $evt.name"
    log.debug "===========|SmartHue|===========   $evt.data"
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset(evt) {
	log.trace "===========|SmartHue|===========   getSunsetOffset $evt.displayName: $evt.name"
    log.debug "===========|SmartHue|===========   $evt.data"
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

def modeChangeHandler(evt) {
	log.trace "===========|SmartHue|===========   modeChangeHandler $evt.name: $evt.value ($triggerModes)"
    log.debug "===========|SmartHue|===========   $evt.data"
	if (evt.value in triggerModes) {
		eventHandler(evt)
	}
}

def scheduledTimeHandler(evt) {
	log.trace "===========|SmartHue|===========   scheduledTimeHandler $evt.displayName: $evt.name"
    log.debug "===========|SmartHue|===========   $evt.data"
	eventHandler(null)
}

def appTouchHandler(evt) {
	log.trace "===========|SmartHue|===========   appTouchHandler $evt.displayName: $evt.name"
    log.debug "===========|SmartHue|===========   $evt.data"
	takeAction(evt)
}

private takeAction(evt) {
	log.trace "===========|SmartHue|===========   takeAction $evt.displayName: $evt.name"
	if (frequency) {
		atomicState[evt.deviceId] = now()
	}

	def hueColor = 0
    def hueSat = 0
    
    switch(color) {
			case "White":
				hueColor = 52
				hueSat = 19
				break;
			case "Daylight":
				hueColor = 53
				hueSat = 91
				break;
			case "Soft White":
				hueColor = 23
				hueSat = 56
				break;
			case "Warm White":
				hueColor = 20
				hueSat = 80 //83
				break;
	 	 	case "Blue":
				hueColor = 70
                hueSat = 100
				break;
			case "Green":
				hueColor = 39
                hueSat = 100
				break;
			case "Yellow":
				hueColor = 25
                hueSat = 100
				break;
			case "Orange":
				hueColor = 10
                hueSat = 100
				break;
			case "Purple":
				hueColor = 75
                hueSat = 100
				break;
			case "Pink":
				hueColor = 83
                hueSat = 100
				break;
			case "Red":
				hueColor = 100
                hueSat = 100
				break;
	}
    if (atomicState.myCounter == null) { atomicState.myCounter = 0}
    atomicState.myCounter = atomicState.myCounter + 1
    
    log.debug "COUNTER: $atomicState.myCounter"
    
    atomicState.previous = [:]
        hues.each {
            atomicState.previous[it.id] = [
                "switch": it.currentValue("switch"),
                "level" : it.currentValue("level"),
                "hue": it.currentValue("hue"),
                "saturation": it.currentValue("saturation"),
                "color": it.currentValue("color")			
            ]
        }
        
        
	log.info "===========|SmartHue|===========   PreviousatomicState: = $atomicState.previous"

	def newValue = [hue: hueColor, saturation: hueSat, level: (lightLevel as Integer) ?: 100]
	log.info "===========|SmartHue|===========   NewatomicState = $newValue"

	hues*.setColor(newValue)
    	if(!duration || onMotionStop == "Yes") 
	{
    	if (onMotionStop == "Yes"){
        	log.info "===========|SmartHue|===========   onMotionStop Override"	
        }
        else{
			log.info "===========|SmartHue|===========   Default Duration Infinite"
        }
	}
    else{
		setTimer(evt)
    }
}

def setTimer(evt){
	log.trace "===========|SmartHue|===========   setTimer $evt.displayName: $evt.name"
	if(!duration || onMotionStop == "Yes") 
	{
    	if (onMotionStop == "Yes"){
        	log.info "===========|SmartHue|===========   onMotionStop Override"	
        }
        else{
			log.info "===========|SmartHue|===========   Default Duration Infinite"
        }
	}
	else if(duration < 0)
	{
		log.info "===========|SmartHue|===========   pause $duration"
		pause(duration * 1000)
		resetHue()
	}
	else
	{
		log.debug "===========|SmartHue|===========   runIn $duration, resetHue"
		runIn(duration,resetHue)
	}
}

def resetHue(){
	atomicState.isRunning = false
	log.trace "===========|SmartHue|===========   resetHue"
	hues.each {
    	log.info "===========|SmartHue|===========   TARGET: $it"
        log.info "===========|SmartHue|===========   PREVIOUS: $atomicState.previous[it.id]"
		it.setColor(atomicState.previous[it.id])    
        
	}
    atomicState.previous = null
}