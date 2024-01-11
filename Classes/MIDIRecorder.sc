MIDIRecorder {
	var <device, <chan, <buffers, <mididefs, key_array;

	*new {
		arg device, chan;
		var buffers, key_array, mididefs;

		MIDIClient.init;

		if (
			device != nil,
			{MIDIIn.connect(device); device = MIDIClient.sources[device]}, {MIDIIn.connectAll; device = "all connected"}
		);

		key_array = Array.newClear(128);
		buffers = Array.new;
		mididefs = [

			// noteOn standard
			MIDIdef.noteOn(\keypress, {
				arg vel, note;
				var timestamp = SystemClock.seconds;
				if (
					key_array[note].size == 0,
					{
						key_array[note] = key_array[note].add([note, vel, timestamp]);
						key_array.postln;
						buffers.postln
					},
					{
						// clear the array with a noteoff and then add note info ... do later
					}
				)

			}),

			// noteOff standard
			MIDIdef.noteOff(\keyup, {
				arg vel, note;
				var timestamp = SystemClock.seconds;
				if (
					key_array[note].size == 1,
					{
						var e;
						e = [note, vel, key_array[note][0][2], timestamp - key_array[note][0][2]];
						// I'm sure there's a more efficient way of getting the index, so I don't need to repeatedly calculate...
						buffers[buffers.size - 1] = buffers[buffers.size - 1].add(e);
						key_array[note] = nil;
						key_array.postln;
						buffers.postln;

					},
					{
						// figure out what the alternative cases are ...
					}
				)
			})

			// add further MIDIdefs if desired...
		];

		// disable mididefs as starting position
		mididefs.do({
			arg mididef;
			mididef.disable
		})

		^super.newCopyArgs(device, chan, buffers, mididefs, key_array)
	}

	record {
		// create buffer, and enable mididefs
		buffers = buffers.add(Array.new);
		mididefs.do({
			arg mididef;
			mididef.enable
		})
	}

	stop {
		mididefs.do({
			arg mididef;
			mididef.disable
		})
		// clean-up ...
	}

	get_keyarray {
		^key_array
	}
}

MIDIRecorderBuf {
	var <buffer;
}

MIDIRecEvent {
	var <note, <vel, <onset, <sus, <delta;

	*new {
		arg note, vel, onset, sus, delta;

		^super.newCopyArgs(note, vel, onset, sus, delta)
	}
}


// MIDIRecorder is like a multitrack midi recorder ...
// MIDIRecBuf is like a record region?
// MIDIRecEvent is basically a midi event that goes in the region.

// the key array works as it is ... so really the midi recorder simply needs a working key array ...

/*m = MIDIRecorder.new()

m.mididefs.do({
	arg mididef;
	mididef.enable
})

m.buffers
m.get_keyarray

MIDIClient.sources*/
