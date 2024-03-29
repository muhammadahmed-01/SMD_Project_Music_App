package com.example.smd_project_music_app.Fragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smd_project_music_app.DB.PlaylistFirebaseDAO;
import com.example.smd_project_music_app.Model.Playlist;
import com.example.smd_project_music_app.Adapter.PlaylistAdapter;
import com.example.smd_project_music_app.PlaylistViewModel;
import com.example.smd_project_music_app.R;
import com.example.smd_project_music_app.Model.Song;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class PlaylistsFragment extends Fragment implements PlaylistAdapter.onPlaylistClick {
		PlaylistsFragmentListener listener;
		private RecyclerView recyclerView;
		private PlaylistAdapter mAdapter;
		private RecyclerView.LayoutManager layoutManager;

		private static ContentResolver contentResolver1;
		private PlaylistFirebaseDAO dao;

		private ArrayList<Playlist> playlists = new ArrayList<>();
		private Hashtable<String, Playlist> index = new Hashtable<String, Playlist>();
		private ArrayList<String> playlistNames = new ArrayList<>();

		private ContentResolver contentResolver;
		private EditText search;
		Filterable filterable;

		private TextView noOfPlaylists;
		private ImageView newPlaylistBtn;

		private File filesDir;

//		private static PlaylistsFragment tabFragment;

//		public static Fragment getInstance(int position, ContentResolver mcontentResolver) {
//				Bundle bundle = new Bundle();
//				bundle.putInt("pos", position);
//				if (tabFragment != null)
//						return tabFragment;
//				tabFragment = new PlaylistsFragment();
//				tabFragment.setArguments(bundle);
//				contentResolver1 = mcontentResolver;
//				return tabFragment;
//		}

		@Override
		public void onCreate(@Nullable Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
		}

		@Override
		public void onAttach(@NonNull Context context) {
				super.onAttach(context);
				if (context instanceof PlaylistsFragmentListener) {
						listener = (PlaylistsFragmentListener) context;
				}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
				View view = inflater.inflate(R.layout.playlist_fragment, container, false);

				search = (EditText) view.findViewById(R.id.search);
				search.addTextChangedListener(new TextWatcher() {
						@Override
						public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

						}

						@Override
						public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
								filterable.getFilter().filter(search.getText().toString());
						}

						@Override
						public void afterTextChanged(Editable editable) {

						}
				});

				recyclerView = view.findViewById(R.id.list);
				recyclerView.setHasFixedSize(true);
				layoutManager = new LinearLayoutManager(getActivity());
				recyclerView.setLayoutManager(layoutManager);

				dao = new PlaylistFirebaseDAO(new PlaylistFirebaseDAO.DataObserver() {
						@Override
						public void update() {
								refresh();
						}
				});
				PlaylistViewModel playlistViewModel = new ViewModelProvider(getActivity()).get(PlaylistViewModel.class);
				playlistViewModel.setDao(dao);
				playlistNames = playlistViewModel.getPlaylistNames(savedInstanceState, "playlistData");
				getPlaylists();

				PlaylistAdapter adapter = new PlaylistAdapter(playlists, this);
				mAdapter = adapter;
				filterable = adapter;
				recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
				recyclerView.setAdapter(mAdapter);

				noOfPlaylists = (TextView) view.findViewById(R.id.no_of_songs);

				newPlaylistBtn = (ImageView) view.findViewById(R.id.new_playlist_btn);
				newPlaylistBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
								LayoutInflater layoutInflater = LayoutInflater.from(getContext());
								View promptView = layoutInflater.inflate(R.layout.new_playlist_prompt, null);

								AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
								alertDialogBuilder.setView(promptView);
								final EditText userInput = (EditText) promptView.findViewById(R.id.editTextDialogUserInput);

								// set dialog message
								alertDialogBuilder
												.setCancelable(false)
												.setPositiveButton("OK",
																new DialogInterface.OnClickListener() {
																		public void onClick(DialogInterface dialog, int id) {
																				// get user input and set it to result
																				// edit text
																				String newPlaylistName = userInput.getText().toString();
																				Playlist playlist = new Playlist(newPlaylistName);
																				String resultMessage = "Successfully created a new playlist!";
																				int hashmapSize = index.size();
																				index.put(playlist.getId(), playlist);
																				if (index.size() > hashmapSize){
																						playlist.setDao(dao);
																						exportToM3U(newPlaylistName, playlist);
																						playlist.save();
																						playlists.add(playlist);
																						playlistNames.add(newPlaylistName);
																						setContent();
																				}
																				else {
																						resultMessage = "A playlist with this name already exists!";
																				}
																				Toast.makeText(getActivity(), resultMessage, Toast.LENGTH_LONG).show();
																		}
																})
												.setNegativeButton("Cancel",
																new DialogInterface.OnClickListener() {
																		public void onClick(DialogInterface dialog, int id) {
																				dialog.cancel();
																		}
																});

								// create alert dialog
								AlertDialog alertDialog = alertDialogBuilder.create();

								// show it
								alertDialog.show();
						}
				});



//				for (int i = 0; i < playlists.size(); i++){
//						exportToM3U(playlists.get(i).getName(), playlists.get(i));
//				}
				filesDir = new File("/storage/emulated/0/Songs/Playlist/");
				if (!filesDir.exists()){
						filesDir.mkdirs();
				}

				return view;
		}

		@Override
		public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
				super.onViewCreated(view, savedInstanceState);
				contentResolver = getActivity().getContentResolver();
				setContent();
		}

		@Override
		public void onItemClick(Playlist playlist) {
				index.put(playlist.getId(), playlist);
				listener.onPlaylistSelected(playlist);
		}

		/**
		 * Setting the content in the listView and sending the data to the Activity
		 */
		public void setContent() {
				getPlaylists();
				mAdapter.notifyDataSetChanged();
				noOfPlaylists.setText(Integer.toString(playlists.size()));
		}

		public void getPlaylists() {
				playlists.clear();
				Playlist playlist;
				for (int i = 0; i < playlistNames.size(); i++) {
						playlist = importPlaylist(playlistNames.get(i));
						playlist.setName(playlistNames.get(i));
						playlists.add(playlist);
				}
		}

		public Playlist importPlaylist(String playlistName) {
				File playlistFile = new File(filesDir, playlistName + ".m3u");
				Playlist playlist = new Playlist();
				try {
						String line;
						BufferedReader reader = new BufferedReader(new FileReader(playlistFile));
						line = reader.readLine();
						if (line != null && line.equals("#EXTM3U")) {
								String artist;
								String title;
								String path;
								int duration;
								while ((line = reader.readLine()) != null && !line.equals("#EXTM3U")) {
										String components[] = line.split(" – ", 2);
										if (components.length > 1) {
												title = components[1];
										} else {
												title = "unknown";
										}
										duration = Integer.parseInt(components[0].split(":")[1].split(",")[0]);
										artist = components[0].split(":")[1].split(",")[1];
										line = reader.readLine();
										path = line;
										playlist.addSong(new Song(title, artist, path, duration));
								}
						} else {
								Log.e("ERROR", "The file is unsupported!");
						}
				} catch (Exception ex) {
						ex.printStackTrace();
				}
				return playlist;
		}

		public void exportToM3U(String playlistName, Playlist playlist) {
				try {
						if (getActivity() == null && filesDir == null)
								return;
						File file = new File(filesDir, playlistName + ".m3u");
						if (file.exists())
								file.delete();
						else if (!file.exists())
								file.createNewFile();
						BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
						writer.append("#EXTM3U");
						writer.newLine();
						String output;
						for (int i = 0; i < playlist.getSongsList().size(); i++) {
								output = "#EXTINF:" + playlist.getDataset().get(i).getDuration() + "," +
												playlist.getDataset().get(i).getArtist() + " – " +
												playlist.getDataset().get(i).getTitle();
								writer.append(output);
								writer.newLine();
								writer.append(playlist.getDataset().get(i).getPath());
								writer.newLine();
						}
						writer.close();
				} catch (Exception ex) {
						ex.printStackTrace();
				}
		}

		public interface PlaylistsFragmentListener {
				public void onPlaylistSelected(Playlist playlist);
		}

		public void refresh() {
				PlaylistViewModel vm = new ViewModelProvider(getActivity()).get(PlaylistViewModel.class);
				if (playlistNames.size() == 0)
						playlistNames = vm.update();
				else {
						Set<String> tempDataset = new HashSet<>(playlistNames);
						playlistNames = vm.update();
						tempDataset.addAll(playlistNames);
						playlistNames.clear();
						playlistNames.addAll(tempDataset);
				}
				if (playlistNames != null) {
						getPlaylists();
						mAdapter.updateData(playlists);
						noOfPlaylists.setText(Integer.toString(playlists.size()));
				}
		}

		public void addSongToPlaylist(Song song){
				if (getActivity() != null){
						AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
						alertDialog.setTitle("Choose playlist");
						ArrayList<String> tempPlaylistNames = new ArrayList<>();
						for (int i = 0; i < playlists.size(); i++){
								tempPlaylistNames.add(playlists.get(i).getName());
						}
						String[] items = tempPlaylistNames.toArray(new String[tempPlaylistNames.size()]);

						alertDialog.setItems(items, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
										Playlist playlist = playlists.get(which);
										int playlistSize = playlist.getSongsList().size();
										playlist.addSong(song);
										if (playlist.getSongsList().size() > playlistSize)
												exportToM3U(playlist.getName(), playlist);
										mAdapter.notifyDataSetChanged();
								}
						});
						AlertDialog alert = alertDialog.create();
						alert.setCanceledOnTouchOutside(false);
						alert.show();
				}
		}

		public void deleteSongsFromPlaylist(String playlistName, ArrayList<String> songPaths){
				ArrayList<Playlist> tempPlaylists = playlists;
				for (int i = 0; i < tempPlaylists.size(); i++){
						if (tempPlaylists.get(i) != null && tempPlaylists.get(i).getName().equals(playlistName)){
								Playlist playlist = tempPlaylists.get(i);
								for (int k = 0; k < songPaths.size(); k++){
										ConcurrentHashMap<String, Song> tempMap = tempPlaylists.get(i).getSongsList();
										for (Map.Entry<String, Song> entry: tempMap.entrySet()){
												if (entry.getValue().getPath().equals(songPaths.get(k))){
														playlist.removeSong(entry.getValue().getId());
												}
										}
								}
								tempPlaylists.set(i, playlist);
								exportToM3U(playlist.getName(), playlist);
						}
				}
				playlists = tempPlaylists;
				mAdapter.notifyDataSetChanged();
		}
}
