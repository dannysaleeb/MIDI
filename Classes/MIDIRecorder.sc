MIDIRecorder {
	var <device, <chan, <buffers, <mididefs, key_array;

	*new {
		arg device, chan;
		var buffers, key_array, mididefs;

		// initialise midi
		MIDIClient.init;

		// if device specified, connect it, otherwise connect all devices
		if (
			device != nil,
			{MIDIIn.connect(device); device = MIDIClient.sources[device]}, {MIDIIn.connectAll; device = "all connected"}
		);

		key_array = Array.newClear(128);
		buffers = Array.new;

		// disable mididefs as starting position
		mididefs.do({
			arg mididef;
			mididef.disable
		})

		^super.newCopyArgs(device, chan, buffers, mididefs, key_array)
	}

	init_mididefs {

		mididefs = [

			// noteOn standard
			MIDIdef.noteOn(\keypress, {
				arg vel, note;
				var timestamp = SystemClock.seconds;
				timestamp.postln;
				if (
					key_array[note].size == 0,
					{
						key_array[note] = key_array[note].add([note, vel, timestamp]);
						timestamp.postln
					},
					{
						// clear the array with a noteoff and then add note info ... do later
					}
				)

			});
			// noteOff standard
			MIDIdef.noteOff(\keyup, {
				arg vel, note;
				var timestamp = SystemClock.seconds;
				timestamp.postln;
				if (
					key_array[note].size == 1,
					{
						var e;
						e = MIDIRecEvent.new(note: note, vel: vel, onset: key_array[note][0][2], sus: timestamp - key_array[note][0][2]);
						timestamp.postln;
						(timestamp - key_array[note][0][2]).postln;
						// I'm sure there's a more efficient way of getting the index, so I don't need to repeatedly calculate...
						buffers[buffers.size - 1] = buffers[buffers.size - 1].add(e);
						key_array[note] = nil;

					},
					{
						// figure out what the alternative cases are ...
					}
				)
			});
		];
	}

	record {
		if (
			buffers.size == 0,
			{ // construce mididefs
				this.init_mididefs; mididefs.do({arg mididef; mididef.disable})
			}
		);
		// create buffer, and enable mididefs
		buffers = buffers.add(Array.new);
		"adding buffer".postln;
		buffers.postln;


		mididefs.do({
			arg mididef;
			mididef.enable
		})
	}

	stop {
		var buffer = buffers[buffers.size - 1];
		mididefs.do({
			arg mididef;
			mididef.disable
		});
		// clean-up ...
		buffer.sort({arg a, b; b.onset > a.onset});
		(buffer.size - 1).do({
			arg i;
			buffer[i].delta = (buffer[i+1].onset - buffer[i].onset)
		});
		// make the last note duration the same as the sustain // or alternatively, I could take a final timestamp at the point at which the recording is stopped, and that could be the final duration ...
		buffer[buffer.size - 1].delta = buffer[buffer.size - 1].sus;
	}

	get_keyarray {
		^key_array
	}
}

// didn't work for some reason ...

// Hm I think this has something to do with the fact that the MIDIdef is defined inside the new method ... so perhaps it's referring to a different buffer???

// try defining the full MIDIdef in the record method? 