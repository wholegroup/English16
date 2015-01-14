/*
 * Copyright (C) 2015 Andrey Rychkov <wholegroup@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wholegroup.android.english16;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** */
public class GameView extends View
{
	/** Игра. */
	private Game mGame;
	
	/** Оступ от края в процентах от диагонали кубика. */
	private static final int BORDER_REL_DIAGONAL = 70;

	/** Коэффициент для рассчета расстояния между клетками. */
	private static final int SPACE_DIV_DIAGONAL = 30;

	/** Игровое поле. */
	private Bitmap mFieldBitmap;

	/** Золотая монета. */
	private Bitmap mCoinGoldBitmap;

	/** Серебрянная монета. */
	private Bitmap mCoinSilverBitmap;

	/** Битмап выделенной клетки. */
	private Bitmap mSelectBitmap;

	/** Диагональ ромба. */
	private int mDiagonal;

	/** Половина диагонали (float). */
	private float mDiagonal2f;

	/** Расстояние между ячейками. */
	private int mSpaceCell;

	/** mDiagonal + mSpaceCell. */
	private float mStepCell;

	/** Координата X начала отрисовки. */
	private float mStartX;

	/** Координата Y начала отрисовки. */
	private float mStartY;

	/** Ширина области вывода. */
	private int mWidth;

	/** Половина ширины области вывода (кэш, float). */
	private float mWidth2f;

	/** Высота области вывода. */
	private int mHeight;

	/** Половина высоты области вывода. */
	private float mHeight2f;

	/** Кисть для вывода ходов. */
	private Paint mPaintMoves;

	/** Координата Y для вывода количества ходов. */
	private float mTextMovesY;

	/** Строка лучшего игрока и сделанных им ходов. */
	private String mBestText;

	/** Координата X выделенной клетки. */
	private int mSelectedX;

	/** Координата Y выделенной клетки. */
	private int mSelectedY;

	/** Обработчик событий ячеек. */
	private ICellListener mCellListener;

	/** Интерфейс обработчика событий ячеек. */
	public interface ICellListener
	{
		abstract void onCellSelected(final int x, final int y);
	}

	/**
	 * Установка обработчика событий ячеек.
	 *
	 * @param cellListener обработчик событий ячеек
	 */
	public void setCellListener(ICellListener cellListener)
	{
		mCellListener = cellListener;
	}

	/**
	 * Устанавливает игру для отображения.
	 *
	 * @param game объект игры
	 */
	public void setGame(Game game)
	{
		mGame = game;
	}

	/**
	 * Конструктор.
	 *
	 * @param context контекст
	 * @param attrs атрибуты
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public GameView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	/**
	 * Отрисовка.
	 *
	 * @param canvas канвас для рисования
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		// отрисовка родителя
		super.onDraw(canvas);

		// генерация битмапов
		if (null == mFieldBitmap)
		{
			initResources();
		}

		// отрисовка поля
		canvas.drawBitmap(mFieldBitmap, 0, 0, null);

		// отрисовка игры
		if (null == mGame)
		{
			return;
		}

		for (int y = 0; y < Game.CELL_COUNT2; y++)
		{
			for (int x = 0; x < Game.CELL_COUNT2; x++)
			{
				final float dx   = mStartX - y * mStepCell + x * mStepCell;
				final float dy   = mStartY + y * mStepCell + x * mStepCell;
				final int   cell = mGame.getCell(x, y);

				switch (cell)
				{
					//
					case Game.COIN_GOLD:
					{
						canvas.drawBitmap(mCoinGoldBitmap, dx, dy, null);

						break;
					}

					//
					case Game.COIN_SILVER:
					{
						canvas.drawBitmap(mCoinSilverBitmap, dx, dy, null);

						break;
					}

					default:
						break;
				}

				// выделенная клетка
				if ((x == mSelectedX) && (y == mSelectedY) && (Game.CELL_DISABLE != cell))
				{
					canvas.drawBitmap(mSelectBitmap, dx, dy, null);
				}
			}
		}

		// отрисовка количества сделанных ходов
		mPaintMoves.setTextAlign(Paint.Align.RIGHT);

		canvas.drawText(getContext().getString(R.string.moves) + " " +
			String.valueOf(mGame.getMoveCount()), mWidth - mSpaceCell, mTextMovesY, mPaintMoves);

		// отрисовка информации лучшего игрока
		if (null != mBestText)
		{
			mPaintMoves.setTextAlign(Paint.Align.LEFT);

			canvas.drawText(mBestText, mSpaceCell, mTextMovesY, mPaintMoves);
		}

		// отрисовка слоя окончания игры
		if (mGame.isEnd())
		{
			drawEndGame(canvas);
		}
	}

	/**
	 * Отрисовывает слой окончания игры.
	 * 
	 * @param canvas канвас для отрисовки
	 */
	private void drawEndGame(final Canvas canvas)
	{
		// ссылка на ресурсы
		final Resources res = getResources();

		// затеняем фон
		canvas.drawColor(res.getColor(R.color.blackout));

		// текст надписи
		final String text = getResources().getString(
			mGame.isSolved() ? R.string.solved : R.string.gameover
		);

		// кисть для надписи
		Paint paintSolved = new Paint(Paint.ANTI_ALIAS_FLAG);

		paintSolved.setTextSize(mDiagonal);
		paintSolved.setStrokeWidth(mSpaceCell);
		paintSolved.setTextAlign(Paint.Align.CENTER);
		paintSolved.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

		// надпись
		paintSolved.setColor(res.getColor(R.color.endgame_text));
		paintSolved.setStyle(Paint.Style.FILL);

		canvas.drawText(text, mWidth2f, mHeight2f - (paintSolved.ascent() +
			paintSolved.descent()) / 2.0f, paintSolved);

		// обводка
		paintSolved.setColor(res.getColor(R.color.endgame_text_border));
		paintSolved.setStyle(Paint.Style.STROKE);

		canvas.drawText(text, mWidth2f, mHeight2f - (paintSolved.ascent() +
			paintSolved.descent()) / 2.0f, paintSolved);
	}

	/**
	 * Обработка изменения размера экрана.
	 *
	 * @param width ширина
	 * @param height высота
	 * @param widthOld старая ширина
	 * @param heightOld старая высота
	 */
	@Override
	protected void onSizeChanged(int width, int height, int widthOld, int heightOld)
	{
		// освобожаем память занимаемую битмапами
		freeResources();

		// передача обработки родителю
		super.onSizeChanged(width, height, widthOld, heightOld);
	}

	/**
	 * Обработка событий тача.
	 *
	 * @param event событие
	 *
	 * @return true, в случае успеха
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// обработка нажатия
		if (MotionEvent.ACTION_DOWN == event.getAction())
		{
			final float posX = event.getX();
			final float posY = event.getY();
			final float xf   = (posX - mStartX - mDiagonal2f + posY - mStartY) / (2 * mStepCell);
			final float yf   = (posY - mStartY - posX + mStartX + mDiagonal2f) / (2 * mStepCell);

			boolean endLoop = false;
			
			while (!endLoop)
			{
				// выход за приделы массива
				if ((xf < 0) || (yf < 0))
				{
					break;
				}

				// перевод в целочисленные координаты
				final int x = (int)xf;
				final int y = (int)yf;

				// выход за приделы массива
				if ((x >= Game.CELL_COUNT2) || (y >= Game.CELL_COUNT2))
				{
					break;
				}

				// пропускаем все клетки, где нет монет
				if ((Game.COIN_GOLD != mGame.getCell(x, y)) && (Game.COIN_SILVER != mGame.getCell(x, y)))
				{
					break;
				}

				// вызываем обработчик
				if (null != mCellListener)
				{
					mCellListener.onCellSelected(x, y);
				}

				endLoop = true;
			}
		}

		return super.onTouchEvent(event);
	}

	/**
	 * Инициализация ресурсов.
	 */
	private void initResources()
	{
		// вычисление различных значений
		calculateValues();

		// инициализация кистей
		initPaints();

		// отрисовка монет
		drawCoinBitmaps();

		// отрисовка поля
		drawFieldBitmap();

		// отрисовка битмапа выделения
		drawSelectedBitmap();
	}

	/**
	 * Отрисовка битмапа выделенной клетки.
	 */
	private void drawSelectedBitmap()
	{
		// освобождение памяти
		if (null != mSelectBitmap)
		{
			mSelectBitmap.recycle();
		}

		// ресурсы
		final Resources res = getResources();

		// битмап
		mSelectBitmap = Bitmap.createBitmap(mDiagonal, mDiagonal, Bitmap.Config.ARGB_8888);

		// канвас
		final Canvas canvas = new Canvas(mSelectBitmap);

		// ромб
		final Path pathRhombus = new Path();

		pathRhombus.moveTo(mDiagonal2f, 0);
		pathRhombus.lineTo(mDiagonal, mDiagonal2f);
		pathRhombus.lineTo(mDiagonal2f, mDiagonal);
		pathRhombus.lineTo(0, mDiagonal2f);

		// закрашиваем
		canvas.clipPath(pathRhombus);
		canvas.drawColor(res.getColor(R.color.selected));
	}

	/**
	 * Отрисовка битмапа поля.
	 */
	private void drawFieldBitmap()
	{
		// удаляем старый битмап
		if (null != mFieldBitmap)
		{
			mFieldBitmap.recycle();
		}

		// ресурсы
		final Resources res = getResources();

		// создаем новый битмап поля
		mFieldBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);

		// канвас для рисования
		final Canvas canvasField = new Canvas(mFieldBitmap);

		// ромб
		final Path pathRhombus = new Path();

		pathRhombus.moveTo(mDiagonal2f, 0);
		pathRhombus.lineTo(mDiagonal, mDiagonal2f);
		pathRhombus.lineTo(mDiagonal2f, mDiagonal);
		pathRhombus.lineTo(0, mDiagonal2f);

		final ShapeDrawable shapeDrawable = new ShapeDrawable(
			new PathShape(pathRhombus, mDiagonal, mDiagonal));

		// светлый кубик
		final Bitmap lightDrawable = BitmapFactory.decodeResource(getResources(), R.drawable.light);
		final Bitmap lightBitmap = Bitmap.createBitmap(mDiagonal, mDiagonal, Bitmap.Config.ARGB_8888);
		final Canvas lightCanvas = new Canvas(lightBitmap);
		final BitmapShader lightBS = new BitmapShader(lightDrawable, Shader.TileMode.REPEAT,
			Shader.TileMode.REPEAT);

		shapeDrawable.getPaint().setShader(lightBS);
		shapeDrawable.setBounds(0, 0, mDiagonal, mDiagonal);
		shapeDrawable.draw(lightCanvas);

		// темный кубик
		final Bitmap darkDrawable = BitmapFactory.decodeResource(getResources(), R.drawable.dark);
		final Bitmap darkBitmap = Bitmap.createBitmap(mDiagonal, mDiagonal, Bitmap.Config.ARGB_8888);
		final Canvas darkCanvas = new Canvas(darkBitmap);
		final BitmapShader darkBS = new BitmapShader(darkDrawable, Shader.TileMode.REPEAT,
			Shader.TileMode.REPEAT);

		shapeDrawable.getPaint().setShader(darkBS);
		shapeDrawable.setBounds(0, 0, mDiagonal, mDiagonal);
		shapeDrawable.draw(darkCanvas);

		// кисти
		final Paint paintWhite = new Paint(Paint.ANTI_ALIAS_FLAG);

		paintWhite.setColor(Color.WHITE);
		paintWhite.setAlpha(100);

		final Paint paintBlack = new Paint(Paint.ANTI_ALIAS_FLAG);

		paintBlack.setColor(Color.BLACK);
		paintBlack.setAlpha(100);

		// кубики 3D (рамки полупрозрачные)
		for (int i = 0; i < mSpaceCell; ++i)
		{
			lightCanvas.drawLine(i, mDiagonal2f, mDiagonal2f, i, paintWhite);
			lightCanvas.drawLine(mDiagonal2f, i, mDiagonal - i, mDiagonal2f, paintWhite);

			lightCanvas.drawLine(mDiagonal - i, mDiagonal2f, mDiagonal2f, mDiagonal - i, paintBlack);
			lightCanvas.drawLine(mDiagonal2f, mDiagonal - i, i, mDiagonal2f, paintBlack);

			darkCanvas.drawLine(i, mDiagonal2f, mDiagonal2f, i, paintWhite);
			darkCanvas.drawLine(mDiagonal2f, i, mDiagonal - i, mDiagonal2f, paintWhite);

			darkCanvas.drawLine(mDiagonal - i, mDiagonal2f, mDiagonal2f, mDiagonal - i, paintBlack);
			darkCanvas.drawLine(mDiagonal2f, mDiagonal - i, i, mDiagonal2f, paintBlack);
		}

		// путь для отрисовки фона
		final int   borderField  = mSpaceCell * 2;
		final float borderIndent = borderField * 1.5f;
		final float ddd = (Game.CELL_COUNT * mDiagonal + Game.CELL_COUNT0 * borderField) / 2.0f;

		Path pathBg = new Path();

		pathBg.moveTo(mWidth2f - 2 * ddd + mDiagonal2f - borderIndent, mHeight2f);
		pathBg.lineTo(mWidth2f - ddd + mDiagonal2f, mHeight2f - ddd - borderIndent);
		pathBg.lineTo(mWidth2f, mHeight2f - mDiagonal2f - borderIndent);
		pathBg.lineTo(mWidth2f + ddd - mDiagonal2f, mHeight2f - ddd - borderIndent);
		pathBg.lineTo(mWidth2f + 2 * ddd - mDiagonal2f + borderIndent, mHeight2f);
		pathBg.lineTo(mWidth2f + ddd - mDiagonal2f, mHeight2f + ddd + borderIndent);
		pathBg.lineTo(mWidth2f, mHeight2f + mDiagonal2f + borderIndent);
		pathBg.lineTo(mWidth2f - ddd + mDiagonal2f, mHeight2f + ddd + borderIndent);
		
		pathBg.close();

		// черный фон
		paintBlack.setAlpha(0xFF);

		canvasField.drawPath(pathBg, paintBlack);

		// рамка фона
		paintWhite.setAlpha(150);
		paintWhite.setStyle(Paint.Style.STROKE);
		paintWhite.setStrokeWidth(mSpaceCell);
		paintWhite.setColor(res.getColor(R.color.field_border));
		
		canvasField.drawPath(pathBg, paintWhite);

		// отрисовываем кубики (ромбы) на поле
		boolean drawDarkBitmap = true;

		for (int y = 0; y < Game.CELL_COUNT2; y++)
		{
			for (int x = 0; x < Game.CELL_COUNT2; x++)
			{
				if (Game.CELL_DISABLE != mGame.getCell(x, y))
				{
					final float dx = mStartX - y * mStepCell + x * mStepCell;
					final float dy = mStartY + y * mStepCell + x * mStepCell;

					canvasField.drawBitmap(drawDarkBitmap ? darkBitmap : lightBitmap, dx, dy, null);
				}

				drawDarkBitmap = !drawDarkBitmap;
			}
		}

		// освобождаем память битмапов кубиков
		lightDrawable.recycle();
		lightBitmap.recycle();
		darkDrawable.recycle();
		darkBitmap.recycle();
	}

	/**
	 * Инициализация кистей.
	 */
	private void initPaints()
	{
		// ресурсы
		final Resources res = getResources();

		// кисть для отрисовки информационных надписей
		mPaintMoves = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		mPaintMoves.setColor(res.getColor(R.color.text));
		mPaintMoves.setTextSize(mDiagonal * BORDER_REL_DIAGONAL / 100.0f / 4.0f);
		mPaintMoves.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
		
		// координата Y для вывода текста с количеством ходов
		mTextMovesY = mHeight - mPaintMoves.getFontMetrics().bottom;
	}

	/**
	 * Вычисляет различные значения.
	 */
	private void calculateValues()
	{
		// размеры экрана
		mWidth    = getWidth();
		mHeight   = getHeight();
		mWidth2f  = mWidth / 2.0f;
		mHeight2f = mHeight / 2.0f;

		// диагональ квадрата (ромба)
		final int diagonalW = (mWidth * 100) / (Game.CELL_COUNT2 * 100 + BORDER_REL_DIAGONAL);
		final int diagonalH = (mHeight * 100) / (Game.CELL_COUNT * 100 + BORDER_REL_DIAGONAL);

		mDiagonal = diagonalW < diagonalH ?
			(0 == diagonalW % 2 ? diagonalW : (diagonalW - 1)):
			(0 == diagonalH % 2 ? diagonalH : (diagonalH - 1));

		mDiagonal2f = mDiagonal / 2.0f;

		// расстояние между клетками
		mSpaceCell = mDiagonal / SPACE_DIV_DIAGONAL;

		// шаг для сдвига одной клетки
		mStepCell = mSpaceCell + mDiagonal2f;

		// координаты левого верхнего угла вывода матрицы (Game.CELL_COUNT x Game.CELL_COUNT)
		mStartX = mWidth2f - mDiagonal2f;
		mStartY = mHeight2f - mDiagonal2f - mStepCell * (Game.CELL_COUNT2 - 1);
	}

	/**
	 * Отрисовка монет.
	 */
	private void drawCoinBitmaps()
	{
		// освобождение памяти
		if (null != mCoinGoldBitmap)
		{
			mCoinGoldBitmap.recycle();
		}

		if (null != mCoinSilverBitmap)
		{
			mCoinSilverBitmap.recycle();
		}

		// ресурсы
		final Resources res = getResources();

		// создание новых битмапов
		mCoinGoldBitmap   = Bitmap.createBitmap(mDiagonal, mDiagonal, Bitmap.Config.ARGB_8888);
		mCoinSilverBitmap = Bitmap.createBitmap(mDiagonal, mDiagonal, Bitmap.Config.ARGB_8888);

		// инициализация переменных
		final Bitmap[] arrCoinBmp = {mCoinGoldBitmap, mCoinSilverBitmap};
		final Path     pathCoin   = new Path();
		final Paint    paintCoin  = new Paint(Paint.ANTI_ALIAS_FLAG);
		final int[]    arrColor   = {
			res.getColor(R.color.coin_gold_start),
			res.getColor(R.color.coin_gold_stop),
			res.getColor(R.color.coin_gold_border),
			res.getColor(R.color.coin_silver_start),
			res.getColor(R.color.coin_silver_stop),
			res.getColor(R.color.coin_silver_border),
		};

		pathCoin.addCircle(mDiagonal2f, mDiagonal2f, mDiagonal2f / 2.0f, Path.Direction.CW);
		paintCoin.setStrokeWidth(1);

		// отрисовка монет
		for (int i = 0; i < arrCoinBmp.length; i++)
		{
			// канвас для рисования
			final Canvas canvasCoin = new Canvas(arrCoinBmp[i]);

			// фишка
			paintCoin.setStyle(Paint.Style.FILL);
			paintCoin.setColor(arrColor[i * 3 + 1]);
			paintCoin.setShader(
				new RadialGradient(
					mDiagonal2f * 3.0f / 4.0f, mDiagonal2f * 3.0f / 4.0f, mDiagonal2f * 3.0f / 4.0f,
					arrColor[i * 3], arrColor[i * 3 + 1],
					Shader.TileMode.CLAMP
				)
			);

			canvasCoin.drawPath(pathCoin, paintCoin);

			// окантовка
			paintCoin.setStyle(Paint.Style.STROKE);
			paintCoin.setColor(arrColor[i * 3 + 2]);
			paintCoin.setShader(null);

			canvasCoin.drawPath(pathCoin, paintCoin);
		}
	}

	/**
	 * Освобождает память занимаемую различными ресурсами.
	 */
	public void freeResources()
	{
		// золотая монета
		if (null != mCoinGoldBitmap)
		{
			mCoinGoldBitmap.recycle();

			mCoinGoldBitmap = null;
		}

		// серебренная монета
		if (null != mCoinSilverBitmap)
		{
			mCoinSilverBitmap.recycle();

			mCoinSilverBitmap = null;
		}

		// поле
		if (null != mFieldBitmap)
		{
			mFieldBitmap.recycle();

			mFieldBitmap = null;
		}

		// выделенная клетка
		if (null != mSelectBitmap)
		{
			mSelectBitmap.recycle();

			mSelectBitmap = null;
		}
	}

	/**
	 * Устанавливает строку лучшего игрока.
	 *
	 * @param bestText строка с рекордом
	 */
	public void setBest(final String bestText)
	{
		if (0 < bestText.length())
		{
			mBestText = bestText;

			invalidate();
		}
	}

	/**
	 * Устанавливает координаты выделенной клетки.
	 *
	 * @param x координата X
	 * @param y координата Y
	 */
	public void setSelected(final int x, final int y)
	{
		mSelectedX = x;
		mSelectedY = y;
	}
}
