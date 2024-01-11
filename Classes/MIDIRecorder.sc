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
						e = MIDIRecEvent.new(note: note, vel: vel, onset: key_array[note][0][2], sus: timestamp - key_array[note][0][2]);
						// I'm sure there's a more efficient way of getting the index, so I don't need to repeatedly calculate...
						buffers[buffers.size - 1] = buffers[buffers.size - 1].add(e);
						key_array[note] = nil;

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
		buffer[buffer.size - 1].delta = buffer[buffer.size - 1].sus
	}

	get_keyarray {
		^key_array
	}
}

/*m = MIDIRecorder.new(chan: 0)

m.record;
m.stop

(
~durs = m.buffers[0].collect({
	arg e;
	e.delta.postln
});

~sus = m.buffers[0].collect({
	arg e;
	e.sus.postln
});

~notes = m.buffers[0].collect({
	arg e;
	e.note.postln
})
)

(
Pbind(
	\midinote, Pseq(~notes + (12 * 4), 1),
	\dur, Pseq(~durs, 1),
	\sus, Pseq(~sus, 1)
).play
)

m.buffers*/

// didn't work for some reason ...