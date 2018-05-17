package com.why94.view.recycler.layoutmanager;

import android.support.annotation.IntRange;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class BigHeadSquareGridLayoutManager extends RecyclerView.LayoutManager {

    public static final int DEFAULT_SPAN_COUNT = 4;
    public static final int DEFAULT_HEAD_WEIGHT = 2;
    public static final int DEFAULT_PRELOAD_LINES = 0;
    public static final int DEFAULT_PREFETCH_LINES = 2;
    public static final int DEFAULT_BORDER_MARGIN = 0;
    public static final int DEFAULT_ITEM_MARGIN = 0;

    private int mSpanCount = DEFAULT_SPAN_COUNT;
    private int mHeadWeight = DEFAULT_HEAD_WEIGHT;
    private int mPreloadLines = DEFAULT_PRELOAD_LINES;
    private int mPrefetchLines = DEFAULT_PREFETCH_LINES;
    private int mBorderMargin = DEFAULT_BORDER_MARGIN;
    private int mItemMargin = DEFAULT_ITEM_MARGIN;

    private int mGridSize;
    private int mVerticalOffset;

    private int mHistoryFirstShowItemPosition = -1, mHistoryLastShowItemPosition = -1;
    private int mHistoryPrefetchFirstShowItemPosition = -1, mHistoryPrefetchLastShowItemPosition = -1;

    private static String tag() {
        return BigHeadSquareGridLayoutManager.class + ":" + System.nanoTime();
    }

    public int getSpanCount() {
        return mSpanCount;
    }

    public BigHeadSquareGridLayoutManager setSpanCount(int spanCount) {
        if (spanCount <= 0) {
            throw new IllegalArgumentException("span count (" + spanCount + ") must > 0");
        }
        if (spanCount < mHeadWeight) {
            throw new IllegalArgumentException("span count (" + spanCount + ") must >= head weight (" + mHeadWeight + ")");
        }
        mSpanCount = spanCount;
        requestLayout();
        return this;
    }

    public int getHeadWeight() {
        return mHeadWeight;
    }

    public BigHeadSquareGridLayoutManager setHeadWeight(int headWeight) {
        if (headWeight <= 0) {
            throw new IllegalArgumentException("head weight (" + headWeight + ") must > 0");
        }
        if (headWeight > mSpanCount) {
            throw new IllegalArgumentException("head weight (" + headWeight + ") must <= span count  (" + mSpanCount + ")");
        }
        mHeadWeight = headWeight;
        requestLayout();
        return this;
    }

    public BigHeadSquareGridLayoutManager setItemMargin(int itemMargin) {
        mItemMargin = itemMargin;
        requestLayout();
        return this;
    }

    public BigHeadSquareGridLayoutManager setBorderMargin(int borderMargin) {
        mBorderMargin = borderMargin;
        requestLayout();
        return this;
    }

    public BigHeadSquareGridLayoutManager setPreloadLines(int preloadLines) {
        mPreloadLines = preloadLines;
        return this;
    }

    public BigHeadSquareGridLayoutManager setPrefetchLines(int prefetchLines) {
        mPrefetchLines = Math.max(0, prefetchLines);
        return this;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() <= 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        if (state.isPreLayout()) {
            return;
        }
        mGridSize = computeGridSize();
        mVerticalOffset = computeVerticalOffset(mVerticalOffset, 0);
        int first = computeFirstShowItemPosition(mVerticalOffset);
        int last = computeLastShowItemPosition(mVerticalOffset);
        reLayoutChildren(recycler, state, first, last);
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int offset = computeVerticalOffset(mVerticalOffset, dy);
        if (offset == mVerticalOffset) {
            return 0;
        }
        dy = mVerticalOffset - offset;
        mVerticalOffset = offset;
        int first = computeFirstShowItemPosition(mVerticalOffset);
        int last = computeLastShowItemPosition(mVerticalOffset);
        if (first != mHistoryFirstShowItemPosition || last != mHistoryLastShowItemPosition) {
            reLayoutChildren(recycler, state, first, last);
        } else {
            offsetChildrenVertical(-dy);
        }
        return dy;
    }

    @Override
    public void scrollToPosition(int position) {
        int gridPosition = toGridPosition(position) + 1;
        int offset = 0;
        if (gridPosition > mSpanCount * mHeadWeight) {
            int linePosition = (int) Math.ceil((float) gridPosition / mSpanCount);
            //add top border
            offset += mBorderMargin;
            //add item space
            offset += (mGridSize + mItemMargin) * (linePosition - 1);
            //to margin center
            offset -= mItemMargin / 2;
        }
        if (-offset < mVerticalOffset - getHeight() + mGridSize + mItemMargin) {
            Log.d(tag(), "scrollToPosition: 目标在下面");
            mVerticalOffset = -offset + getHeight() - mGridSize - mItemMargin;
            requestLayout();
        } else if (-offset > mVerticalOffset) {
            Log.d(tag(), "scrollToPosition: 目标在上面");
            mVerticalOffset = -offset;
            requestLayout();
        } else {
            Log.d(tag(), "scrollToPosition: 目标在中间");
        }
    }

    private void reLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state, int firstShowItemPosition, int lastItemShowPosition) {
        mHistoryFirstShowItemPosition = firstShowItemPosition;
        mHistoryLastShowItemPosition = lastItemShowPosition;
        int left, top;
        detachAndScrapAttachedViews(recycler);
        //layout head view
        if (firstShowItemPosition == 0) {
            left = mBorderMargin;
            top = mBorderMargin + mVerticalOffset;
            int size = mHeadWeight * mGridSize + (mHeadWeight - 1) * mItemMargin;
            View view = recycler.getViewForPosition(0);
            measureChild(view, size, size);
            layoutDecorated(view, left, top, left + size, top + size);
            addView(view);
        }
        //layout normal view
        int firstLine = toGridPosition(firstShowItemPosition) / mSpanCount;
        for (int y = firstLine, position = Math.max(1, firstShowItemPosition); position <= lastItemShowPosition; y++) {
            for (int x = 0; x < mSpanCount && position <= lastItemShowPosition; x++, position++) {
                if (x < mHeadWeight && y < mHeadWeight) {
                    position--;
                    continue;
                }
                left = mBorderMargin + x * mGridSize + x * mItemMargin;
                top = mBorderMargin + y * mGridSize + y * mItemMargin + mVerticalOffset;
                View view = recycler.getViewForPosition(position);
                measureChild(view, mGridSize, mGridSize);
                layoutDecorated(view, left, top, left + mGridSize, top + mGridSize);
                addView(view);
            }
        }
    }

    @Override
    public void measureChild(View child, int width, int height) {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        lp.width = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        lp.height = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        child.measure(lp.width, lp.height);
    }

    @Override
    public void collectAdjacentPrefetchPositions(int dx, int dy, RecyclerView.State state, LayoutPrefetchRegistry layoutPrefetchRegistry) {
        if (mHistoryPrefetchFirstShowItemPosition != mHistoryFirstShowItemPosition) {
            mHistoryPrefetchFirstShowItemPosition = mHistoryFirstShowItemPosition;
            for (int i = Math.max(0, mHistoryPrefetchFirstShowItemPosition - mPrefetchLines * mSpanCount); i < mHistoryPrefetchFirstShowItemPosition; i++) {
                layoutPrefetchRegistry.addPosition(i, 0);
            }
        }
        if (mHistoryPrefetchLastShowItemPosition != mHistoryLastShowItemPosition) {
            mHistoryPrefetchLastShowItemPosition = mHistoryLastShowItemPosition;
            for (int i = Math.min(getItemCount() - 1, mHistoryPrefetchLastShowItemPosition + mPrefetchLines * mSpanCount); i > mHistoryPrefetchLastShowItemPosition; i--) {
                layoutPrefetchRegistry.addPosition(i, 0);
            }
        }
    }

    private int computeGridSize() {
        float width = getWidth();
        //remove border
        width = width - mBorderMargin * 2;
        //remove item margin
        width = width - mItemMargin * (mSpanCount - 1);
        return (int) Math.ceil(width / mSpanCount);
    }

    /**
     * 转换子项序号到单元格序号, 因为头项可能占用多个单元格, 所以子项的序号不一定等于单元格序号
     *
     * @return 头项单元格位置为0, 子项单元格位置根据头项所占比重动态计算
     */
    @IntRange(from = 0)
    private int toGridPosition(int position) {
        if (position == 0) {
            return 0;
        } else {
            return position + mHeadWeight * mHeadWeight - 1;
        }
    }

    private int computeFirstShowItemPosition(int offset) {
        offset = -offset;
        //remove top border
        offset = offset - mBorderMargin;
        int line = (int) (Math.ceil((float) offset / (mGridSize + mItemMargin)));
        //add preload lines
        line = line - mPreloadLines;
        if (line <= mHeadWeight) {
            return 0;
        }
        int maxGridPosition = line * mSpanCount - 1;
        int maxPosition = maxGridPosition - mHeadWeight * mHeadWeight + 1;
        //min position
        return maxPosition - (mSpanCount - 1);
    }

    private int computeLastShowItemPosition(int offset) {
        offset = -offset + getHeight();
        int lastShowLine = (int) (Math.ceil((float) (offset - mBorderMargin + mItemMargin) / (mGridSize + mItemMargin)));
        lastShowLine += mPreloadLines;
        int maxGridPosition = lastShowLine * mSpanCount - 1;
        int maxPosition = maxGridPosition - mHeadWeight * mHeadWeight + 1;
//        int maxPosition = lastShowLine * mSpanCount - mHeadWeight * mHeadWeight;
        if (maxPosition < 0) {
            return 0;
        }
        return Math.min(maxPosition, getItemCount() - 1);
    }

    private int computeVerticalOffset(int current, int offset) {
        if (current - offset > 0) {
            Log.d(tag(), "computeVerticalOffset: 到达顶部");
            return 0;
        } else {
            int contentHeight = computeContentHeight();
            int maximumAllowedOffset = getHeight() - contentHeight;
            if (maximumAllowedOffset > 0) {
                Log.d(tag(), "computeVerticalOffset: 不可滑动");
                return 0;
            } else if (current - offset < maximumAllowedOffset) {
                Log.d(tag(), "computeVerticalOffset: 到达底部");
                return maximumAllowedOffset;
            } else {
                Log.d(tag(), "computeVerticalOffset: 正常滑动");
                return current - offset;
            }
        }
    }

    private int computeContentHeight() {
        int gridCount = toGridPosition(getItemCount());
        int gridLines = (int) Math.ceil((float) gridCount / mSpanCount);
        return mBorderMargin + gridLines * (mGridSize + mItemMargin) - mItemMargin + mBorderMargin;
    }
}