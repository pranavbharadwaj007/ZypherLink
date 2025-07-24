export namespace main {
	
	export class PairedDevice {
	    device_id: string;
	    device_name: string;
	    device_type: string;
	    // Go type: time
	    paired_at: any;
	    // Go type: time
	    last_seen: any;
	    is_online: boolean;
	    trust_level: string;
	    transfer_count: number;
	
	    static createFrom(source: any = {}) {
	        return new PairedDevice(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.device_id = source["device_id"];
	        this.device_name = source["device_name"];
	        this.device_type = source["device_type"];
	        this.paired_at = this.convertValues(source["paired_at"], null);
	        this.last_seen = this.convertValues(source["last_seen"], null);
	        this.is_online = source["is_online"];
	        this.trust_level = source["trust_level"];
	        this.transfer_count = source["transfer_count"];
	    }
	
		convertValues(a: any, classs: any, asMap: boolean = false): any {
		    if (!a) {
		        return a;
		    }
		    if (a.slice && a.map) {
		        return (a as any[]).map(elem => this.convertValues(elem, classs));
		    } else if ("object" === typeof a) {
		        if (asMap) {
		            for (const key of Object.keys(a)) {
		                a[key] = new classs(a[key]);
		            }
		            return a;
		        }
		        return new classs(a);
		    }
		    return a;
		}
	}
	export class TransferRecord {
	    id: string;
	    sender_name: string;
	    file_count: number;
	    total_size: number;
	    duration_ms: number;
	    success: boolean;
	    // Go type: time
	    timestamp: any;
	    save_location: string;
	
	    static createFrom(source: any = {}) {
	        return new TransferRecord(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.id = source["id"];
	        this.sender_name = source["sender_name"];
	        this.file_count = source["file_count"];
	        this.total_size = source["total_size"];
	        this.duration_ms = source["duration_ms"];
	        this.success = source["success"];
	        this.timestamp = this.convertValues(source["timestamp"], null);
	        this.save_location = source["save_location"];
	    }
	
		convertValues(a: any, classs: any, asMap: boolean = false): any {
		    if (!a) {
		        return a;
		    }
		    if (a.slice && a.map) {
		        return (a as any[]).map(elem => this.convertValues(elem, classs));
		    } else if ("object" === typeof a) {
		        if (asMap) {
		            for (const key of Object.keys(a)) {
		                a[key] = new classs(a[key]);
		            }
		            return a;
		        }
		        return new classs(a);
		    }
		    return a;
		}
	}

}

