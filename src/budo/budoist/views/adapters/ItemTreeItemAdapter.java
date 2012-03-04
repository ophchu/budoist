package budo.budoist.views.adapters;

import java.util.ArrayList;
import java.util.Hashtable;
import pl.polidea.treeview.AbstractTreeViewAdapter;
import budo.budoist.R;
import budo.budoist.models.Item;
import budo.budoist.models.Label;
import budo.budoist.models.Project;
import budo.budoist.models.TodoistTextFormatter;
import budo.budoist.services.TodoistClient;
import budo.budoist.services.TodoistOfflineStorage;
import budo.budoist.views.ItemListView;
import budo.budoist.views.ItemListView.ItemViewMode;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Tree item adapter for a single item in the items list 
 * 
 */
public class ItemTreeItemAdapter extends AbstractTreeViewAdapter<Item> implements OnClickListener {

	private ItemListView mItemListView;
	private ItemViewMode mItemViewMode;
	
	private TodoistClient mClient;
	private TodoistOfflineStorage mStorage;
	
	private int mTextSize;
	
	public interface IOnItemCompleted { void onItemCompleted(Item item, boolean value); }
	public interface IOnItemNotes { void onItemNotes(Item item); }
	
	private IOnItemCompleted mOnItemCompleted = null;
	private IOnItemNotes mOnItemNotes = null;
	
	private Hashtable<Integer, Label> mIdToLabels;
    
    private OnCheckedChangeListener onCheckedChange = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView,
                final boolean isChecked) {
            final Item item = (Item)buttonView.getTag();
           	RelativeLayout viewLayout = (RelativeLayout)buttonView.getParent().getParent();
	        TextView itemContent = (TextView) viewLayout
	        	.findViewById(R.id.item_list_item_content);
  
            if (isChecked) {
	        	itemContent.setTextColor(Color.GRAY);
	        } else {
		        itemContent.setTextColor((0xFF << 24) | item.getItemPriorityColor());
	        }
            
            if (mOnItemCompleted != null) {
	            mOnItemCompleted.onItemCompleted(item, isChecked);
            }
        }
    };
    
    /**
     * Updates the labels list - used when the user updates the labels (so visuallym, we'll have
     * to show the new labels for items)
     * 
     * @param labels
     */
    public void setLabels(ArrayList<Label> labels) {
        // Create a label-id ==> label mapping
        mIdToLabels = new Hashtable<Integer, Label>();
        
        for (int i = 0; i < labels.size(); i++) {
        	mIdToLabels.put(labels.get(i).id, labels.get(i));
        }
    }
   
    public ItemTreeItemAdapter(final ItemListView itemListView,
    		final IOnItemCompleted onItemCompleted,
    		final IOnItemNotes onItemNotes,
    		final ArrayList<Label> labels,
            final TreeStateManager<Item> treeStateManager,
            final int numberOfLevels) {
        super(itemListView, treeStateManager, numberOfLevels);
        
        mClient = itemListView.getClient();
        mStorage = mClient.getStorage();
        
        mOnItemCompleted = onItemCompleted;
        mOnItemNotes = onItemNotes;
        
        mItemListView = itemListView;
        mItemViewMode = mItemListView.getViewMode();

        setLabels(labels);
    }

    @Override
    public View getNewChildView(final TreeNodeInfo<Item> treeNodeInfo) {
        final LinearLayout viewLayout = (LinearLayout) getActivity()
                .getLayoutInflater().inflate(R.layout.item_list_item, null);
        return updateView(viewLayout, treeNodeInfo);
    }

    @Override
    public View updateView(final View view,
            final TreeNodeInfo<Item> treeNodeInfo) {
    	Item item = treeNodeInfo.getId();
    	LinearLayout viewLayout = (LinearLayout) view;

        TextView itemContent = (TextView) viewLayout
        	.findViewById(R.id.item_list_item_content);
        TextView itemLabels = (TextView) viewLayout
			.findViewById(R.id.item_list_item_labels);
        ImageView itemNotes = (ImageView) viewLayout
        	.findViewById(R.id.item_list_item_notes);
        TextView itemNoteCount = (TextView) viewLayout
        	.findViewById(R.id.item_list_item_note_count);
        ImageView itemRepeat = (ImageView) viewLayout
        	.findViewById(R.id.item_list_item_repeat);
        TextView itemDueDate = (TextView) viewLayout
        	.findViewById(R.id.item_list_item_due_date);
        LinearLayout itemDateLayout = (LinearLayout) viewLayout
        	.findViewById(R.id.item_list_item_date_layout);
        final CheckBox itemCheckbox = (CheckBox) viewLayout
        	.findViewById(R.id.item_list_item_checkbox);
        
        mTextSize = mStorage.getTextSize();
        
        // Display the formatted text (highlighting, etc)
        itemContent.setText(TodoistTextFormatter.formatText(item.getContent()));
    	
        itemContent.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mTextSize);
        // Linkify any emails, web addresses and phone numbers in the item's content
        Linkify.addLinks(itemContent, Linkify.ALL);
        
        
        itemLabels.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mTextSize - 4);
        
        if (mItemViewMode == ItemViewMode.FILTER_BY_LABELS) {
        	// Show item's project
        	Project project = mClient.getProjectById(item.projectId);
        	itemLabels.setText(project.getName());
        	itemLabels.setBackgroundColor((0xFF << 24) | project.getColor());
        	
        } else {
	        itemLabels.setText("");
        	itemLabels.setBackgroundColor((0x00 << 24)); // Transparent background color
        	
        	// Show item's labels
	        if (item.labelIds != null) {
		        // Fill out the labels
		        for (int i = 0; i < item.labelIds.size(); i++) {
		        	int labelId = item.labelIds.get(i);
		        	Label currentLabel = mIdToLabels.get(labelId);
		        	
		        	itemLabels.append(Html.fromHtml(String.format("<font color='#%X'><i>%s</i></font>",
		        			currentLabel.getColor(), currentLabel.name)));
		        	
		        	if (i < item.labelIds.size() - 1) {
		        		itemLabels.append(", ");
		        	}
		        }
		        
		        // Since Italic text gets cut off on "wrap_contents" TextView width
		        itemLabels.append(" ");
	        }
        }
	        
        if (mClient.isPremium()) {
	        itemNoteCount.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mTextSize - 7);
			if (item.noteCount > 0) {
	        	itemNotes.setVisibility(View.VISIBLE);
	        	itemNoteCount.setText(String.valueOf(item.noteCount));
	        	itemNoteCount.setVisibility(View.VISIBLE);
			} else {
				// No notes (don't show an icon)
	        	itemNotes.setVisibility(View.GONE);
	        	itemNoteCount.setVisibility(View.GONE);
			}
			
	        itemNotes.setTag(item);
			itemNotes.setOnClickListener(this);
	        	
        } else {
        	// Only premium users have task notes
        	itemNotes.setVisibility(View.GONE);
        	itemNoteCount.setVisibility(View.GONE);
        }
        
        // Show a "recurring" image for item if necessary
        if (item.isRecurring()) {
        	itemRepeat.setVisibility(View.VISIBLE);
        } else {
        	itemRepeat.setVisibility(View.GONE);
        }
        
        itemDueDate.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mTextSize - 4);
        
        if ((item.dueDate == null) || (item.dueDate.getTime() == 0)) {
        	itemDateLayout.setVisibility(View.GONE);
        } else {
        	itemDateLayout.setVisibility(View.VISIBLE);
        	itemDueDate.setText(item.getDueDateDescription(mClient.getUser().timeFormat));
        	itemDateLayout.setBackgroundColor((0xFF << 24) | item.getDueDateColor());
        }
        
        itemCheckbox.setTag(item);
	    itemCheckbox.setOnCheckedChangeListener(null);
        itemCheckbox.setChecked(item.completed);
        
        int checkBoxDrawable;
        int checkBoxHeight;
        
        if (mTextSize <= 10) {
        	checkBoxDrawable = R.drawable.checkbox_selector_small;
        	checkBoxHeight = 16;
        } else if (mTextSize <= 13) {
        	checkBoxDrawable = R.drawable.checkbox_selector_medium;
        	checkBoxHeight = 24;
        } else {
        	checkBoxDrawable = R.drawable.checkbox_selector_large;
        	checkBoxHeight = 32;
        }
        
        itemCheckbox.setButtonDrawable(checkBoxDrawable);
        itemCheckbox.setLayoutParams(new LinearLayout.LayoutParams(checkBoxHeight, checkBoxHeight));
        
        if (item.completed)
        	itemContent.setTextColor(Color.GRAY);
        else
	        itemContent.setTextColor((0xFF << 24) | item.getItemPriorityColor());
 
        
	    itemCheckbox.setOnCheckedChangeListener(onCheckedChange);
	    
	    viewLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				itemCheckbox.setChecked(!itemCheckbox.isChecked());
			}
		});
	    
	    viewLayout.setLongClickable(true);
	    
        viewLayout.setTag(item);

        return viewLayout;
    }
    
    @Override
    public Drawable getBackgroundDrawable(final TreeNodeInfo<Item> treeNodeInfo) {
    	return new ColorDrawable(Color.WHITE);
    }

    @Override
    public long getItemId(final int position) {
        return getTreeId(position).id;
    }

	@Override
	public void onClick(View arg0) {
		// Notes icon was clicked
		if (mOnItemNotes != null) {
			mOnItemNotes.onItemNotes((Item)arg0.getTag());
		}
		
	}
}
