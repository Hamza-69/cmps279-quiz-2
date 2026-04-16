package lb.edu.aub.cmps279Spring26.hmr23.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Shared across all fragments via ViewModelProvider(requireActivity()).
 * Carries the selected book ID from List → Detail/Edit,
 * and a refresh flag from Add/Edit/Delete → List.
 */
public class SharedViewModel extends ViewModel {

    private final MutableLiveData<String> selectedBookId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> listNeedsRefresh = new MutableLiveData<>(false);

    public MutableLiveData<String> getSelectedBookId() {
        return selectedBookId;
    }

    public void setSelectedBookId(String id) {
        selectedBookId.setValue(id);
    }

    public MutableLiveData<Boolean> getListNeedsRefresh() {
        return listNeedsRefresh;
    }

    public void requestListRefresh() {
        listNeedsRefresh.setValue(true);
    }

    public void consumeRefresh() {
        listNeedsRefresh.setValue(false);
    }
}
