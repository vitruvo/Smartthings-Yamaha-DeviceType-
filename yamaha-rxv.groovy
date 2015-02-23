/**
 *  Yamaha RX V1071 Remote
 *
 *  Copyright 2015 m.becker@vitruvo.com
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
 */
metadata {
	definition (name: "YAMAHA RX V1071 Remote", namespace: "yamaharxv", author: "m.becker@vitruvo.com") {
		capability "musicPlayer"
		capability "refresh"
		capability "switch"        
        
        command "volumeup"
        command "volumedown"
        command "volumewhisper"
        command "partymodeon"
        command "partymodeoff"
	}

    preferences {
	    input("DeviceIP", "string", title:"IP Address", description: "Please enter the IP Address of the Receiver", required: true, displayDuringSetup: true)
    	input("DevicePort", "number", title:"Port", description: "Please enter Port", defaultValue: 80 , required: true, displayDuringSetup: true)
	    input("APIEndpoint", "string", title:"Path to the API Endpoint", description: "Please enter the path to the API Endpoint", defaultValue: "/YamahaRemoteControl/ctrl", required: true, displayDuringSetup: true)
	}
    
	simulator {
    
	}

	tiles {
	    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on",  icon: "st.Electronics.electronics19", backgroundColor: "#ffffff", nextState: "on"
			state "on",  label: 'On',  action: "switch.off", icon: "st.Electronics.electronics19", backgroundColor: "#79b821", nextState: "off"
		}

		standardTile("volume", "device.volume", width: 1, height: 1)
        {
            state "default", label: 'Up', action: "device.volumeup", backgroundColor: "#ffffff", icon: "st.thermostat.thermostat-up"
        }
        
		standardTile("volume", "device.volume", width: 1, height: 1)
        {
            state "default", label: 'Down', action: "device.volumeup", backgroundColor: "#000000", icon: "st.thermostat.thermostat-down"
        }


		standardTile("volume", "device.volume", width: 1, height: 1)
        {
            state "default", label: 'Mute', action: "Music Player.mute", backgroundColor: "#ffffff", icon: "st.quirky.spotter.quirky-spotter-sound-off"
        }
        
		standardTile("mode", "device.modes", width: 1, height: 1)
        {
            state "default", label: 'Mute', action: "partymodeon", backgroundColor: "#ffffff", icon: "st.quirky.spotter.quirky-spotter-sound-off"
        }


        
		main "switch"
		details(["switch", "refresh", "mute"])
	}
    
    /*
    st.thermostat.thermostat-up
    st.thermostat.thermostat-down
    st.quirky.spotter.quirky-spotter-sound-off
    
    
    */
}

def parse(String description) {
    log.debug "Parsing '${description}'"
    def map = stringToMap(description)

    def result = []

    if (map.bucket && map.key) { //got a s3 pointer
    	putImageInS3(map)
    }
    else if (map.headers && map.body) { //got device info response
    	def headerString = new String(map.headers.decodeBase64())
    	if (headerString.contains("404 Not Found")) {
    		state.snapshot = "/snapshot.cgi"
   		}

    	if (map.body) {
        def bodyString = new String(map.body.decodeBase64())
        def body = new XmlSlurper().parseText(bodyString)
        def productName = body?.productName?.text()
    	if (productName) {
            log.trace "Product Name: $productName"
            state.snapshot = ""
        }
    }
    }

    result
}

// handle commands
def on() {
    log.debug("Switching Device On") 
	sendCommand('data:<YAMAHA_AV cmd="PUT"><System><Power_Control><Power>On</Power></Power_Control></System></YAMAHA_AV>')
}

def off() {
    log.debug("Switching Device Off") 
	sendCommand('data:<YAMAHA_AV cmd="PUT"><System><Power_Control><Power>Standby</Power></Power_Control></System></YAMAHA_AV>')
}

def mute() {
	log.debug("Mute Toggle")
    sendCommand('data:<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Mute>On/Off</Mute></Volume></Main_Zone></YAMAHA_AV>')
}

def unmute() {
	log.debug("Unmute")
    sendCommand('data:<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Mute>Off</Mute></Volume></Main_Zone></YAMAHA_AV>')
}

def volumeup(){
	log.debug("Turning Volume Up for Main Zone")
    sendCommand('data:<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Lvl><Val>Up 3 dB</Val><Exp></Exp><Unit></Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>')
}

def volumedown(){
	log.debug("Turning Volume Down for Main Zone")
    sendCommand('data:<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Lvl><Val>Down 3 dB</Val><Exp></Exp><Unit></Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>')
}

def volumewhisper(){
	log.debug("Whisper Volume")
    sendCommand('data:<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Lvl><Val>-400</Val><Exp></Exp><Unit></Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>')
}


def partymodeon(){
	log.debug("Partymode On")
    sendCommand('data:<YAMAHA_AV cmd="PUT"><System><Party_Mode><Mode>On</Mode></Party_Mode></System></YAMAHA_AV>')
}

def partymodeoff(){
	log.debug("Partymode Off")
    sendCommand('data:<YAMAHA_AV cmd="PUT"><System><Party_Mode><Mode>Off</Mode></Party_Mode></System></YAMAHA_AV>')
}




// Command Sender
private sendCommand(command) {
	log.debug("executing command...") 

	def host = DeviceIP 
    def hosthex = convertIPtoHex(host)
    def porthex = convertPortToHex(DevicePort)
    device.deviceNetworkId = "$hosthex:$porthex"

	def path = APIEndpoint
    
    
    def headers = [:] 
    headers.put("HOST", "$host:$DevicePort")
    headers.put("Content-Type", "text/xml")

    def method = "POST"

    def result = new physicalgraph.device.HubAction(
        method: method,
        path: path,
        body: command,
        headers: headers
    )

    result
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	log.debug "IP address entered is $ipAddress:$port and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
/*	String hexport = port.toString().format( '%04x', port.toInteger() )*/
    String hexport = 80
/*    log.debug hexport */
    return hexport
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}


private String convertHexToIP(hex) {
//	log.debug("Convert hex to ip: $hex") 
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
//    log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}