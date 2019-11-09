# Image2Audio

Image2Audio is a web application that interprets image data as musical information and synthesizes audio for the tune.

Note when creating songs- images with lots of varying colors, like "psychedelic" patterns, produce much more interesting songs!

WARNING: Generated tunes are loud right now. Get ready with that volume control!

Users can register an account easily and publish their songs for others to view.

# Technical details

At its core, Image2Audio is a Java program.

Command-line arguments supply the program with an image URL, a waveshape, a musical scale, and an audio output URL.
The image is downscaled and the resized image is sampled for RGB values of each pixel.
For each pixel, a musical note is created with the red value controlling pitch, the green value defining note duration, and the blue value setting the volume.
Sound wave data is generated based on the provided waveshape, and this data is written to the audio output file using the javax.sound.sampled package.
The front-end web experience uses HTML/CSS, PHP, and a small amount of JavaScript to receive parameters from the user and pass them to the Java program, which is executed on the server. The WAV file created by the Java program is then converted to MP3 using the LAME codec.

A PHP page diplays the resulting audio file alongside the source image and gives the user the option to publish his or her song.

Publishing a song stores references to the image and audio files to a MySQL database with the name of the song's author. It also performs a hidden "cleanup" routine which deletes unpublished audio files that are more than 2 hours old, if the last cleanup was more than 24 hours ago.

User accounts are also stored in a MySQL database, and user profiles can be viewed to show the age of their account and any songs that they have published.

# License

Image2Audio is licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
