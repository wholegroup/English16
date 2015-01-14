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

import android.util.Log;
import biz.source_code.base64Coder.Base64Coder;

import java.io.*;

/**
 * Класс игры.
 */
public class Game
{
	/** Наименование класса для лога. */
	public static final String TAG = "Game";

	/** Максимальное количество ходов. */
	public static final int MAX_MOVES = 99;

	/** Количество кубиков в квадрате. */
	public static final int CELL_COUNT  = 3;
	public static final int CELL_COUNT0 = CELL_COUNT - 1;
	public static final int CELL_COUNT2 = CELL_COUNT * 2 - 1;

	/** Нерабочая ячейка. */
	public static final int CELL_DISABLE = -1;

	/** Пустая ячейка. */
	public static final int CELL_EMPTY = 0;

	/** Золотая ячейка. */
	public final static int COIN_GOLD = 1;

	/** Серебрянная ячейка. */
	public final static int COIN_SILVER = 2;

	/** Игровое поле. */
	private final int[][] mGameField = new int[CELL_COUNT2][CELL_COUNT2];

	/** Количество совершенных ходов. */
	private int mMoveCount;

	/** Флаг решения головоломки. */
	private boolean mSolved;

	/** Флаг окочания игры. */
	private boolean mGameOver;

	/**
	 * Конструктор по-умолчанию
	 */
	public Game()
	{
		create();
	}

	/**
	 * Инициализация игры.
	 */
	public void create()
	{
		// флаг окончания игры
		mGameOver = false;

		// флаг нерешенности головоломки
		mSolved = false;

		// обнуляем число ходов
		mMoveCount = 0;

		// формирование игрового поля
		for (int y = 0; y < CELL_COUNT2; y++)
		{
			for (int x = 0; x < CELL_COUNT2; x++)
			{
				// не используемые клетки
				if ((y < CELL_COUNT0) && (x < CELL_COUNT0))
				{
					mGameField[y][x] = CELL_DISABLE;
				}

				// неиспользуемые клетки
				else if ((y > CELL_COUNT0) && (x > CELL_COUNT0))
				{
					mGameField[y][x] = CELL_DISABLE;
				}

				// клетки с серебрянными монетами
				else if ((y <= CELL_COUNT0) && (x >= CELL_COUNT0))
				{
					mGameField[y][x] = COIN_SILVER;
				}

				// клетки с золотыми монетами
				else
				{
					mGameField[y][x] = COIN_GOLD;
				}
			}
		}

		mGameField[CELL_COUNT - 1][CELL_COUNT - 1] = CELL_EMPTY;
	}

	/**
	 * Возвращает флаг окончания игры.
	 *
	 * @return флаг окончания игры.
	 */
	public boolean isGameOver()
	{
		return mGameOver;
	}

	/**
	 * Возвращает флаг решения головоломки.
	 *
	 * @return флаг решения головоломки.
	 */
	public boolean isSolved()
	{
		return mSolved;
	}

	/**
	 * Возвращает флаг окончания игры.
	 *
	 * @return флаг окончания игры.
	 */
	public boolean isEnd()
	{
		return isGameOver() || isSolved();
	}

	/**
	 * Выполняем ход по указанным координатам.
	 *
	 * @param x кордината X
	 * @param y координата Y
	 *
	 * @return true, в случае успеха
	 */
	public boolean moveTo(int x, int y)
	{
		// выход, если игра не запущена
		if (isEnd())
		{
			return false;
		}

		// сдвигаем монету
		if (!moveCoin(x, y))
		{
			return false;
		}

		// увеличиваем количество ходов
		mMoveCount++;

		// проверяем решение
		if (checkSolved())
		{
			mSolved = true;
		}

		// проверяем максимальное количество ходов
		if (!mSolved && (mMoveCount >= MAX_MOVES))
		{
			mGameOver = true;
		}

		return true;
	}

	/**
	 * Выполняет передвижение монет по указанным координатам.
	 *
	 * @param x координата X в массиве
	 * @param y координата Y в массиве
	 *
	 * @return true, если передвижение было выполнено
	 */
	private boolean moveCoin(final int x, final int y)
	{
		// поиск свободной клетки вокруг монеты
		final int[] checkX = {x - 1, x, x + 1, x, x - 2, x, x + 2, x};
		final int[] checkY = {y, y - 1, y, y + 1, y, y - 2, y, y + 2};

		for(int i = 0; i < 8; i++)
		{
			// позиция для проверки
			final int cx = checkX[i];
			final int cy = checkY[i];

			// проверка выхода за границу
			if ((cx < 0) || (cy < 0))
			{
				continue;
			}

			if ((cx >= CELL_COUNT2) || (cy >= CELL_COUNT2))
			{
				continue;
			}

			// клетка должна быть пустой
			if (CELL_EMPTY != mGameField[cy][cx])
			{
				continue;
			}

			// выполняем передвижение монеты
			mGameField[cy][cx] = mGameField[y][x];
			mGameField[y][x]   = CELL_EMPTY;

			return true;
		}

		return false;
	}


	/**
	 * Проверка решения.
	 *
	 * @return true, если головоломка решена
	 */
	private boolean checkSolved()
	{
		for (int y = 0; y < CELL_COUNT2; y++)
		{
			for (int x = 0; x < CELL_COUNT2; ++x)
			{
				// пропуск неиспользуемых клеток
				if (CELL_DISABLE == mGameField[y][x])
				{
					continue;
				}

				// если в левой нижней части есть золотые монеты, значит игра не закончена
				if ((y >= CELL_COUNT0) && (x < CELL_COUNT) && (COIN_GOLD== mGameField[y][x]))
				{
					return false;
				}

				// если в правой верхней части есть серебрянные монеты, значит игра не закончена
				if ((y < CELL_COUNT) && (x >= CELL_COUNT0) && (COIN_SILVER == mGameField[y][x]))
				{
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Возвращает значение клетки по указанным координатам.
	 *
	 * @param x координата X
	 * @param y координата Y
	 *
	 * @return значение клетки
	 */
	public int getCell(final int x, final int y)
	{
		return mGameField[y][x];
	}

	/**
	 * Серилизует данные в строку Base64.
	 *
	 * @return строка в кодировке Base64
	 */
	public String toBase64()
	{
		try
		{
			// серилизуем массив
			final ByteArrayOutputStream os  = new ByteArrayOutputStream();
			final ObjectOutputStream    out = new ObjectOutputStream(os);

			out.writeObject(mGameField);
			out.writeBoolean(mSolved);
			out.writeBoolean(mGameOver);
			out.writeInt(mMoveCount);

			out.close();

			return new String(Base64Coder.encode(os.toByteArray()));
		}
		catch (IOException exception)
		{
			Log.e(TAG, exception.toString());
		}

		return "";
	}

	/**
	 * Восстанавливает данные объекта из строки в Base64.
	 *
	 * @param data строка в Base64
	 */
	public void fromBase64(String data)
	{
		try
		{
			final ByteArrayInputStream is = new ByteArrayInputStream(Base64Coder.decode(data));
			final ObjectInputStream    in = new ObjectInputStream(is);

			final int[][] gameField = (int[][])in.readObject();
			mSolved = in.readBoolean();
			mGameOver = in.readBoolean();
			mMoveCount = in.readInt();

			in.close();

			// копируем массив
			for (int y = 0 ; y < CELL_COUNT2; y++)
			{
				System.arraycopy(gameField[y], 0, mGameField[y], 0, CELL_COUNT2);
			}
		}
		catch (ClassNotFoundException exception)
		{
			Log.e(TAG, exception.toString());
		}
		catch (IOException exception)
		{
			Log.e(TAG, exception.toString());
		}
	}

	/**
	 * Возвращает количество сделанных ходов.
	 *
	 * @return количество сделанных ходов
	 */
	public int getMoveCount()
	{
		return mMoveCount;
	}

}
