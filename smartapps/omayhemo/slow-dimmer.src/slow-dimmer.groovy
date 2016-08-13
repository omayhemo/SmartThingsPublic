    definition(
        name: "Slow Dimmer",
        namespace: "omayhemo",
        author: "Doug Beard",
        description: "Slowly Dim a Light by Schedule",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
    
    preferences {
    	section("Select dimmers to slowly dim...") {
    		input "dimmers", "capability.switchLevel", title: "Which?", required: true, multiple: true
    	}
        
        section("Over how many minutes to dim...") {
        	input "minutes", "number", title: "Minutes?", required: true, multiple: false
        }
        
        section("Select momentary button to launch...") {
        	input "trigger", "capability.momentary", title: "Which?", required: true
        }
        
         section("At what dim level should we start...") {
        	input "starting", "number", title: "Start?", required: true, multiple: false
        }
        
        section("At what dim level should we end...") {
        	input "ending", "number", title: "End?", required: true, multiple: false
        }
    }
    
    def installed() {
    	initialize()
    }
    
    def updated() {
    	unsubscribe()
    	initialize()
    }
    
    def initialize() {
    	subscribe(trigger, "switch.on", triggerHandler)
    }
    
def triggerHandler(evt) {
	log.debug "Triggered"
    dimmers.each{
    	log.debug "Dimmer: ${it.name}"
    }
    
    log.debug "Starting at: ${starting}"
    
   /*   if(dimmers[0].currentSwitch == "off") state.currentLevel = 100
    else state.currentLevel = dimmers[0].currentLevel */
  state.currentLevel = starting
  log.debug "CurrentLevel == ${state.currentLevel}"
  log.debug "EndingLevel: {$ending}"
  log.debug "Dimming Over: ${minutes}"
    if(minutes == 0) return
    state.dimStep = state.currentLevel - ending
    state.dimStep = state.dimStep / minutes
    log.debug "DimStepping: ${state.dimStep}"
    state.dimLevel = state.currentLevel
    dimmers.setLevel(starting)
    runIn(30, dimStep)
    
}

def dimStep() {
	if(state.currentLevel > ending) {
        state.dimLevel = state.dimLevel - state.dimStep
        if (state.dimLevel > ending)
        {
            log.debug "DimStep: ${state.dimLevel}"
            state.currentLevel = state.dimLevel.toInteger()
            dimmers.setLevel(state.currentLevel)
            runIn(60,dimStep)
        }
    }
}