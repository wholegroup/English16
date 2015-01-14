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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;

/**
 * Главная активити с игрой.
 */
public class GameActivity extends Activity
{
	/** Игра. */
	private Game mGame;

	/** Представление игры. */
	private GameView mGameView;

	/** Пул звуков. */
	private SoundPool mSounds;

	/** Номера звуков. */
	private int[] mSoundsN;

	/**
	 * Индексы номеров звуков.
	 */
	private interface Sound
	{
		/** Количество индексов. */
		public static final int COUNT = 3;

		/** Индекс номера звука - сделан ход. */
		public static final int MOVE = 0;

		/** Индекс номера звука - конец игры. */
		public static final int GAME_OVER = 1;

		/** Индекс номера звука - игра пройдена. */
		public static final int RESOLVED = 2;
	}

	/** Имя лучшего игрока. */
	private String mBestName;

	/** Количество ходов сделанных лучшим игроком. */
	private int mBestMoves;

	/** Флаг звука. */
	private boolean mSoundOn;

	/** Звук по-умолчанию включен. */
	private static final boolean DEFAULT_SOUND = true;

	/** Флаг первого запуска. Служит для вывода информационного диалога при старте. */
	private boolean mFirstRun;

	/** Координата X выделенной клетки. */
	private int mSelectX;

	/** Координата Y выделенной клетки. */
	private int mSelectY;

	/**
	 * Создание активити
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// установка слоя
		setContentView(R.layout.main);

		// игра
		mGame = new Game();

		// ссылка на представление игры
		mGameView = ((GameView)findViewById(R.id.game_view));

		// сокрытие выделения клетки
		hideSelected();

		// загрузка настроек игры
		loadSettings();

		// восстановление состояния игры
		if (null != savedInstanceState)
		{
			mGame.fromBase64(savedInstanceState.getString(getString(R.string.game_instance) + "-mGame"));

			mSelectX = savedInstanceState.getInt(getString(R.string.game_instance) + "-mSelectX");
			mSelectY = savedInstanceState.getInt(getString(R.string.game_instance) + "-mSelectY");
		}
		
		// установка рекорда
		if (0 < mBestName.length())
		{
			mGameView.setBest(String.format(getString(R.string.score_best), mBestName, mBestMoves));
		}

		// установка обработчика клеток
		mGameView.setCellListener(new MyCellListener());
		
		// передача представлению ссылки на игру
		mGameView.setGame(mGame);

		// координаты выделенной клетки
		mGameView.setSelected(mSelectX, mSelectY);

		// вызов информационного диалога при первом запуске
		if (mFirstRun)
		{
			mFirstRun = false;

			showAboutDialog();
		}
	}

	/**
	 * Удаление активити.
	 */
	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		// освобождение ресурсов
		releaseSounds();
	}

	/**
	 * Создание меню.
	 *
	 * @param menu меню
	 *
	 * @return true, в случае успеха
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// создаем меню из ресурсов
		getMenuInflater().inflate(R.menu.options, menu);

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Подготовка меню перед выводом.
	 *
	 * @param menu меню
	 *
	 * @return true, в случае успеха
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		// изменяем текст в пункте переключения звука
		final MenuItem itemSound = menu.findItem(R.id.menu_sound);

		if (null != itemSound)
		{
			itemSound.setTitle(mSoundOn ? getString(R.string.menu_sound_off) :
				getString(R.string.menu_sound_on));
		}

		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Обработка выбора пунктов меню.
	 * 
	 * @param item пункт меню
	 *
	 * @return true, в случае успеха
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			// новая игра
			case R.id.menu_new:
			{
				startNewGame();
				
				return true;
			}

			// информаци об игре
			case R.id.menu_about:
			{
				showAboutDialog();

				return true;
			}

			//
			case R.id.menu_feedback:
			{
				final Intent email = new Intent(android.content.Intent.ACTION_SEND);

				email.setType("plain/text");

				email.putExtra(
					android.content.Intent.EXTRA_EMAIL,
					new String[] {getString(R.string.feedback_email)}
				);
				email.putExtra(
					android.content.Intent.EXTRA_SUBJECT,
					getString(R.string.feedback_subject)
				);
				email.putExtra(
					android.content.Intent.EXTRA_TEXT,
					getString(R.string.feedback_body)
				);

				startActivity(Intent.createChooser(email, getString(R.string.feedback_dialog)));

				return true;
			}

			// переключение звука
			case R.id.menu_sound:
			{
				mSoundOn = !mSoundOn;

				if (mSoundOn)
				{
					loadSounds();
				}
				else
				{
					releaseSounds();
				}

				return true;
			}

			default:
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Сохранение состояния игры.
	 *
	 * @param outState бандл с состоянием игры
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		outState.putString(getString(R.string.game_instance) + "-mGame", mGame.toBase64());
		outState.putInt(getString(R.string.game_instance) + "-mSelectX", mSelectX);
		outState.putInt(getString(R.string.game_instance) + "-mSelectY", mSelectY);
	}

	/**
	 * Обработка установки на паузу.
	 */
	@Override
	protected void onPause()
	{
		// освобождение ресурсов представления
		mGameView.freeResources();

		// удаление звуков
		releaseSounds();

		// сохраняем настройки
		saveSettings();

		super.onPause();
	}

	/**
	 * Обработка возобновления игры.
	 */
	@Override
	protected void onResume()
	{
		// загрузка звуков
		if (mSoundOn)
		{
			loadSounds();
		}

		super.onResume();
	}

	/**
	 * Запускает новую игру.
	 */
	private void startNewGame()
	{
		// создание игры
		mGame.create();

		// сокрытие выделения
		hideSelected();

		// перерисовка
		mGameView.invalidate();
	}

	/**
	 * Загрузка звуков.
	 */
	private void loadSounds()
	{
		if (null != mSounds)
		{
			throw new IllegalArgumentException();
		}

		mSoundsN = new int[Sound.COUNT];
		mSounds  = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);

		mSoundsN[Sound.MOVE]      = mSounds.load(this, R.raw.move, 0);
		mSoundsN[Sound.GAME_OVER] = mSounds.load(this, R.raw.game_over, 0);
		mSoundsN[Sound.RESOLVED]  = mSounds.load(this, R.raw.solved, 0);
	}

	/**
	 * Выгрузка звуков.
	 */
	private void releaseSounds()
	{
		if (null != mSounds)
		{
			mSounds.release();

			mSounds = null;
		}
	}

	/**
	 * Проигрывает указанный звук.
	 *
	 * @param sound индекс номера звука
	 */
	private void playSound(final int sound)
	{
		mSounds.play(mSoundsN[sound], 1, 1, 1, 0, 1);
	}

	/**
	 * Обработчик клеток.
	 */
	private class MyCellListener implements GameView.ICellListener
	{
		/**
		 * Выбор клетки.
		 *
 		 * @param x позиция X
		 * @param y позиция Y
		 */
		public void onCellSelected(final int x, final int y)
		{
			// скрываем выделение клетки
			hideSelected();
			
			moveCell(x, y);
		}
	}

	/**
	 * Перемещает указанную клетку.
	 *
	 * @param x координата X
	 * @param y координата Y
	 */
	private void moveCell(final int x, final int y)
	{
		// выполняем ход
		if (!mGame.moveTo(x, y))
		{
			return;
		}

		// проигрывание звука
		if (mSoundOn)
		{
			// игра окончена
			if (mGame.isGameOver())
			{
				playSound(Sound.GAME_OVER);
			}

			// головоломка решена
			else if (mGame.isSolved())
			{
				playSound(Sound.RESOLVED);
			}

			// сделан ход
			else
			{
				playSound(Sound.MOVE);
			}
		}

		// перерисовываем игру
		mGameView.invalidate();

		// диалог ввода имени
		if (mGame.isSolved() && (mGame.getMoveCount() < mBestMoves))
		{
			showNameDialog();
		}
	}

	/**
	 * Создает и выводит диалог ввода имени пользователя.
	 */
	private void showNameDialog()
	{
		// установка имени из предыдущего рекорда
		final View     yourNameView = View.inflate(this, R.layout.score, null);
		final TextView nameView     = (TextView)yourNameView.findViewById(R.id.score_name);

		nameView.setText(mBestName);

		// создание диалога
		new AlertDialog.Builder(this)
			.setTitle(getString(R.string.score_header))
			.setView(yourNameView)
			.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						final String name = nameView.getText().toString();

						if (0 < name.length())
						{
							mBestName  = name;
							mBestMoves = mGame.getMoveCount();
							
							// устанавливаем строку рекорда
							mGameView.setBest(String.format(getString(R.string.score_best),
								mBestName, mBestMoves));
						}
					}
				})
			.show();
	}


	/**
	 * Загрузка настроек игры.
	 */
	private void loadSettings()
	{
		final String            id       = getString(R.string.preferences_id); 
		final SharedPreferences settings = getSharedPreferences(id, 0);

		mBestName  = settings.getString(id + "-mBestName", "");
		mBestMoves = settings.getInt(id + "-mBestMoves", Game.MAX_MOVES);
		mSoundOn   = settings.getBoolean(id + "-mSoundOn", DEFAULT_SOUND);
		mFirstRun  = settings.getBoolean(id + "-mFirstRun", true);
	}

	/**
	 * Сохранение настроек игры.
	 */
	private void saveSettings()
	{
		final String                   id       = getString(R.string.preferences_id);
		final SharedPreferences        settings = getSharedPreferences(id, 0);
		final SharedPreferences.Editor editor   = settings.edit();

		editor.putString(id + "-mBestName", mBestName);
		editor.putInt(id + "-mBestMoves", mBestMoves);
		editor.putBoolean(id + "-mSoundOn", mSoundOn);
		editor.putBoolean(id + "-mFirstRun", mFirstRun);

		editor.commit();
	}

	/**
	 * Выводит информационный диалог.
	 */
	private void showAboutDialog()
	{
		new AlertDialog.Builder(this)
			.setView(View.inflate(this, R.layout.about, null))
			.setTitle(R.string.about_header)
			.setPositiveButton(getString(R.string.ok), null)
			.show();
	}

	/**
	 * Обработка нажатия кнопок.
	 *
	 * @param keyCode код клавиши
	 * @param event событие
	 *
	 * @return true, если обработка выполнена
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			//
			case KeyEvent.KEYCODE_DPAD_LEFT:
			{
				moveSelected(mSelectX, mSelectY + 1);

				return true;
			}

			//
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			{
				moveSelected(mSelectX, mSelectY - 1);

				return true;
			}

			//
			case KeyEvent.KEYCODE_DPAD_UP:
			{
				moveSelected(mSelectX - 1, mSelectY);

				return true;
			}

			//
			case KeyEvent.KEYCODE_DPAD_DOWN:
			{
				moveSelected(mSelectX + 1, mSelectY);

				return true;
			}

			//
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_SPACE:
			{
				moveCell(mSelectX, mSelectY);

				return true;
			}

			default:
				break;
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Перемещает выделение клетки.
	 *
	 * @param x новая координата X
	 * @param y новая координата Y
	 *
	 * @return true, если перемещение было выполнено
	 */
	private boolean moveSelected(final int x, final int y)
	{
		// проверка текущих координат
		if ((0 > mSelectX) || (0 > mSelectY)
				|| (Game.CELL_COUNT2 <= mSelectX) || (Game.CELL_COUNT2 <= mSelectY)
				|| (Game.CELL_DISABLE == mGame.getCell(mSelectX, mSelectY)))
		{
			mSelectX = 0;
			mSelectY = Game.CELL_COUNT2 - 1;

			mGameView.setSelected(mSelectX, mSelectY);
			mGameView.invalidate();

			return true;
		}

		// проверка выхода за границы массива новых координат
		if ((0 > x) || (0 > y) || (Game.CELL_COUNT2 <= x) || (Game.CELL_COUNT2 <= y))
		{
			return false;
		}

		// проверка рабочей клетки 
		if (Game.CELL_DISABLE == mGame.getCell(x, y))
		{
			return false;
		}

		// устанавливаем новые координаты выделенной клетки
		mSelectX = x;
		mSelectY = y;

		// обновляем представление игры
		mGameView.setSelected(x, y);
		mGameView.invalidate();

		return true;
	}

	/**
	 * Скрывает выделение клетки.
	 */
	private void hideSelected()
	{
		mSelectX = 0;
		mSelectY = 0;

		mGameView.setSelected(mSelectX, mSelectY);
	}
}
