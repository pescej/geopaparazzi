package eu.geopaparazzi.map.utils;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.woxthebox.draglistview.BoardView;
import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragItemAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.types.ESpatialDataSources;
import eu.geopaparazzi.map.R;

public class BoardFragment extends Fragment implements IActivitySupporter, View.OnClickListener {

    public static final int PICKFILE_REQUEST_CODE = 666;
    public static final int PICKFOLDER_REQUEST_CODE = 667;

    private static int sCreatedItems = 0;
    private BoardView mBoardView;
    private int mColumns;

    private boolean isFabOpen = false;
    private FloatingActionButton toggleButton, addSourceButton, addSourceFolderButton;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward;

    public static BoardFragment newInstance() {
        return new BoardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.board_layout, container, false);

        mBoardView = view.findViewById(R.id.board_view);
        mBoardView.setSnapToColumnsWhenScrolling(true);
        mBoardView.setSnapToColumnWhenDragging(true);
        mBoardView.setSnapDragItemToTouch(true);
        mBoardView.setCustomDragItem(new MyDragItem(getActivity(), R.layout.column_item));
        mBoardView.setCustomColumnDragItem(new MyColumnDragItem(getActivity(), R.layout.column_drag_layout));
        mBoardView.setSnapToColumnInLandscape(false);
        mBoardView.setColumnSnapPosition(BoardView.ColumnSnapPosition.CENTER);
        mBoardView.setBoardListener(new BoardView.BoardListener() {
            @Override
            public void onItemDragStarted(int column, int row) {
                //Toast.makeText(getContext(), "Start - column: " + column + " row: " + row, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemDragEnded(int fromColumn, int fromRow, int toColumn, int toRow) {
                if (fromColumn != toColumn || fromRow != toRow) {
                    //Toast.makeText(getContext(), "End - column: " + toColumn + " row: " + toRow, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onItemChangedPosition(int oldColumn, int oldRow, int newColumn, int newRow) {
                //Toast.makeText(mBoardView.getContext(), "Position changed - column: " + newColumn + " row: " + newRow, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemChangedColumn(int oldColumn, int newColumn) {
                TextView itemCount1 = mBoardView.getHeaderView(oldColumn).findViewById(R.id.item_count);
                itemCount1.setText(String.valueOf(mBoardView.getAdapter(oldColumn).getItemCount()));
                TextView itemCount2 = mBoardView.getHeaderView(newColumn).findViewById(R.id.item_count);
                itemCount2.setText(String.valueOf(mBoardView.getAdapter(newColumn).getItemCount()));
            }

            @Override
            public void onFocusedColumnChanged(int oldColumn, int newColumn) {
                //Toast.makeText(getContext(), "Focused column changed from " + oldColumn + " to " + newColumn, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onColumnDragStarted(int position) {
                //Toast.makeText(getContext(), "Column drag started from " + position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onColumnDragChangedPosition(int oldPosition, int newPosition) {
                //Toast.makeText(getContext(), "Column changed from " + oldPosition + " to " + newPosition, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onColumnDragEnded(int position) {
                //Toast.makeText(getContext(), "Column drag ended at " + position, Toast.LENGTH_SHORT).show();
            }
        });
        mBoardView.setBoardCallback(new BoardView.BoardCallback() {
            @Override
            public boolean canDragItemAtPosition(int column, int dragPosition) {
                // Add logic here to prevent an item to be dragged
                return true;
            }

            @Override
            public boolean canDropItemAtPosition(int oldColumn, int oldRow, int newColumn, int newRow) {
                // Add logic here to prevent an item to be dropped
                return oldColumn == newColumn;
            }
        });


        toggleButton = view.findViewById(R.id.mapToggleButton);
        toggleButton.setOnClickListener(this);
        addSourceButton = view.findViewById(R.id.addMapSourceButton);
        addSourceButton.setOnClickListener(this);
        addSourceFolderButton = view.findViewById(R.id.addMapSourceFolderButton);
        addSourceFolderButton.setOnClickListener(this);

        fab_open = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_backward);

        return view;
    }


    @Override
    public void onClick(View v) {
        if (v == toggleButton) {
            animateFAB();
        } else if (v == addSourceButton) {
            addMap();
        } else if (v == addSourceFolderButton) {
            addMapFolder();
        }
    }


    public void animateFAB() {
        if (isFabOpen) {
            toggleButton.startAnimation(rotate_backward);
            addSourceButton.startAnimation(fab_close);
            addSourceFolderButton.startAnimation(fab_close);
            addSourceButton.setClickable(false);
            addSourceFolderButton.setClickable(false);
            isFabOpen = false;
        } else {
            toggleButton.startAnimation(rotate_forward);
            addSourceButton.startAnimation(fab_open);
            addSourceFolderButton.startAnimation(fab_open);
            addSourceButton.setClickable(true);
            addSourceFolderButton.setClickable(true);
            isFabOpen = true;
        }
    }

    public void addMap() {
        try {
            String title = "Add map...";// getString(R.string.select_basemap_source);
            String[] supportedExtensions = ESpatialDataSources.getAllSupportedExtensions();
            AppsUtilities.pickFile(this, PICKFILE_REQUEST_CODE, title, supportedExtensions, null);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(getActivity(), e, null);
        }
    }

    public void addMapFolder() {
        try {
            String title = "Add maps folder...";//getString(R.string.select_basemap_source_folder);
            String[] supportedExtensions = ESpatialDataSources.getAllSupportedExtensions();
            AppsUtilities.pickFolder(this, PICKFOLDER_REQUEST_CODE, title, null, supportedExtensions);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(getActivity(), e, null);
        }
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Board");

//        public static final int MAPSFORGE_PRE = 0;
//        public static final int DEFAULT_ONLINE_SERVICES = 1;
//        public static final int OVERLAYS = 2;
//        public static final int GEOPAPARAZZI = 3;
//        public static final int OBJECTS3D = 4;
//        public static final int SYSTEM = 5;
//        public static final int ON_TOP_GEOPAPARAZZI = 6;
//        public static final int ON_TOP_SYSTEM = 7;

        addColumn("mapsforge");
        addColumn("default online serivices");
        addColumn("overlays");
        addColumn("project layers");
        addColumn("3D layers");
        addColumn("system layers");
        addColumn("above project");
        addColumn("above system");
    }

    private void addColumn(String name) {
        final ArrayList<Pair<Long, String>> mItemArray = new ArrayList<>();

        final int column = mColumns;
        final ItemAdapter listAdapter = new ItemAdapter(mItemArray, R.layout.column_item, R.id.item_layout, true);
        final View header = View.inflate(getActivity(), R.layout.column_header, null);
        ((TextView) header.findViewById(R.id.text)).setText(name);
        ((TextView) header.findViewById(R.id.item_count)).setText("");
        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long id = sCreatedItems++;
                Pair item = new Pair<>(id, "Test " + id);
                mBoardView.addItem(mBoardView.getColumnOfHeader(v), 0, item, true);
                //mBoardView.moveItem(4, 0, 0, true);
                //mBoardView.removeItem(column, 0);
                //mBoardView.moveItem(0, 0, 1, 3, false);
                //mBoardView.replaceItem(0, 0, item1, true);
                ((TextView) header.findViewById(R.id.item_count)).setText(String.valueOf(mItemArray.size()));
            }
        });
        mBoardView.addColumn(listAdapter, header, header, false);
        mColumns++;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.menu_board, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
//        menu.findItem(R.id.action_disable_drag).setVisible(mBoardView.isDragEnabled());
//        menu.findItem(R.id.action_enable_drag).setVisible(!mBoardView.isDragEnabled());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_disable_drag:
//                mBoardView.setDragEnabled(false);
//                getActivity().invalidateOptionsMenu();
//                return true;
//            case R.id.action_enable_drag:
//                mBoardView.setDragEnabled(true);
//                getActivity().invalidateOptionsMenu();
//                return true;
//            case R.id.action_add_column:
//                addColumn();
//                return true;
//            case R.id.action_remove_column:
//                mBoardView.removeColumn(0);
//                return true;
//            case R.id.action_clear_board:
//                mBoardView.clearBoard();
//                return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public FragmentManager getSupportFragmentManager() {
        return getFragmentManager();
    }


    private static class MyColumnDragItem extends DragItem {

        MyColumnDragItem(Context context, int layoutId) {
            super(context, layoutId);
            setSnapToTouch(false);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            LinearLayout clickedLayout = (LinearLayout) clickedView;
            View clickedHeader = clickedLayout.getChildAt(0);
            RecyclerView clickedRecyclerView = (RecyclerView) clickedLayout.getChildAt(1);

            View dragHeader = dragView.findViewById(R.id.drag_header);
            ScrollView dragScrollView = dragView.findViewById(R.id.drag_scroll_view);
            LinearLayout dragLayout = dragView.findViewById(R.id.drag_list);
            dragLayout.removeAllViews();

            ((TextView) dragHeader.findViewById(R.id.text)).setText(((TextView) clickedHeader.findViewById(R.id.text)).getText());
            ((TextView) dragHeader.findViewById(R.id.item_count)).setText(((TextView) clickedHeader.findViewById(R.id.item_count)).getText());
            for (int i = 0; i < clickedRecyclerView.getChildCount(); i++) {
                View view = View.inflate(dragView.getContext(), R.layout.column_item, null);
                ((TextView) view.findViewById(R.id.text)).setText(((TextView) clickedRecyclerView.getChildAt(i).findViewById(R.id.text)).getText());
                dragLayout.addView(view);

                if (i == 0) {
                    dragScrollView.setScrollY(-clickedRecyclerView.getChildAt(i).getTop());
                }
            }

            dragView.setPivotY(0);
            dragView.setPivotX(clickedView.getMeasuredWidth() / 2);
        }

        @Override
        public void onStartDragAnimation(View dragView) {
            super.onStartDragAnimation(dragView);
            dragView.animate().scaleX(0.9f).scaleY(0.9f).start();
        }

        @Override
        public void onEndDragAnimation(View dragView) {
            super.onEndDragAnimation(dragView);
            dragView.animate().scaleX(1).scaleY(1).start();
        }
    }

    private static class MyDragItem extends DragItem {

        MyDragItem(Context context, int layoutId) {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            CharSequence text = ((TextView) clickedView.findViewById(R.id.text)).getText();
            ((TextView) dragView.findViewById(R.id.text)).setText(text);
            CardView dragCard = dragView.findViewById(R.id.card);
            CardView clickedCard = clickedView.findViewById(R.id.card);

            dragCard.setMaxCardElevation(40);
            dragCard.setCardElevation(clickedCard.getCardElevation());
            // I know the dragView is a FrameLayout and that is why I can use setForeground below api level 23
            dragCard.setForeground(clickedView.getResources().getDrawable(R.drawable.card_view_drag_foreground));
        }

        @Override
        public void onMeasureDragView(View clickedView, View dragView) {
            CardView dragCard = dragView.findViewById(R.id.card);
            CardView clickedCard = clickedView.findViewById(R.id.card);
            int widthDiff = dragCard.getPaddingLeft() - clickedCard.getPaddingLeft() + dragCard.getPaddingRight() -
                    clickedCard.getPaddingRight();
            int heightDiff = dragCard.getPaddingTop() - clickedCard.getPaddingTop() + dragCard.getPaddingBottom() -
                    clickedCard.getPaddingBottom();
            int width = clickedView.getMeasuredWidth() + widthDiff;
            int height = clickedView.getMeasuredHeight() + heightDiff;
            dragView.setLayoutParams(new FrameLayout.LayoutParams(width, height));

            int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
            dragView.measure(widthSpec, heightSpec);
        }

        @Override
        public void onStartDragAnimation(View dragView) {
            CardView dragCard = dragView.findViewById(R.id.card);
            ObjectAnimator anim = ObjectAnimator.ofFloat(dragCard, "CardElevation", dragCard.getCardElevation(), 40);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.setDuration(ANIMATION_DURATION);
            anim.start();
        }

        @Override
        public void onEndDragAnimation(View dragView) {
            CardView dragCard = dragView.findViewById(R.id.card);
            ObjectAnimator anim = ObjectAnimator.ofFloat(dragCard, "CardElevation", dragCard.getCardElevation(), 6);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.setDuration(ANIMATION_DURATION);
            anim.start();
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (PICKFILE_REQUEST_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String filePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                        File file = new File(filePath);
                        if (file.exists()) {
                            Utilities.setLastFilePath(getActivity(), filePath);
                            final File finalFile = file;
                            // TODO do something with file


                            int focusedColumn = mBoardView.getFocusedColumn();
                            int itemCount = mBoardView.getItemCount(focusedColumn);
                            Pair<Long, String> pair = new Pair<>((long) 1, finalFile.getName());

                            mBoardView.addItem(focusedColumn, itemCount, pair, true);
                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(getActivity(), e, null);
                    }
                }
                break;
            }
            case (PICKFOLDER_REQUEST_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String folderPath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                        final File folder = new File(folderPath);
                        if (folder.exists()) {
                            Utilities.setLastFilePath(getContext(), folderPath);
                            final List<File> foundFiles = new ArrayList<>();
                            // get all supported files
                            String[] supportedExtensions = ESpatialDataSources.getSupportedTileSourcesExtensions();
                            FileUtilities.searchDirectoryRecursive(folder, supportedExtensions, foundFiles);
                            // add basemap to list and in mPreferences
                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(getActivity(), e, null);
                    }
                }
                break;
            }
        }
    }
}