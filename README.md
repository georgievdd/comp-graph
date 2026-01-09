# Лабораторные работы по компьютерной графике

Проект содержит реализацию лабораторных работ по компьютерной графике на Java.

## Структура проекта

```
labs/
├── Lab1.java  - Смешивание изображений
├── Lab2.java  - Алгоритм рассеивания ошибки (Floyd-Steinberg)
├── Lab3.java  - Построение и заполнение полигонов
├── Lab4.java  - Кривые Безье, отсечение отрезков
├── Lab5.java  - 3D проекции и анимация
└── HomeWork.java - Домашняя работа
```

## Запуск лабораторных работ

```bash
cd labs
java Lab1.java  # Лабораторная работа №1
java Lab2.java  # Лабораторная работа №2
java Lab3.java  # Лабораторная работа №3
java Lab4.java  # Лабораторная работа №4
```

---

## Лабораторная работа №1: Смешивание изображений

### Задание

1. Круглое полутоновое изображение
2. Смешивание двух изображений 8 bpp с альфа-каналом
3. Зеркальное отражение и транспонирование

### Результаты

#### Синтетические изображения
![img.png](res/img1.png)

#### Работа с реальными фотографиями

| Оригинал | Круговая маска |
|----------|----------------|
| ![test_img.png](res/test_img.png) | ![test_img_circular.png](res/test_img_circular.png) |

| Оригинал | Зеркальное отражение |
|----------|----------------------|
| ![test_img.png](res/test_img.png) | ![test_img_flipped.png](res/test_img_flipped.png) |

| Результат смешивания |
|---------------------|
| ![test_img_blended.png](res/test_img_blended.png) |

---

## Лабораторная работа №2: Алгоритм рассеивания ошибки

### Задание

1. Преобразование изображения 8 bpp в n bpp алгоритмом Floyd-Steinberg
2. Проход в противоположном направлении для чётных и нечётных строк

### Результаты

#### Синтетические изображения
![img.png](res/img2.png)

#### Работа с реальными фотографиями

| Оригинал (цветной) | Grayscale 8 bpp |
|-------------------|-----------------|
| ![test_img.png](res/test_img.png) | ![test_photo_original_gray.png](res/test_photo_original_gray.png) |

**1 bpp (2 уровня яркости):**

| Стандартный Floyd-Steinberg | С чередующимся сканированием |
|----------------------------|------------------------------|
| ![test_photo_1bpp.png](res/test_photo_1bpp.png) | ![test_photo_1bpp_alternating.png](res/test_photo_1bpp_alternating.png) |

**2 bpp (4 уровня яркости):**

| Стандартный Floyd-Steinberg | С чередующимся сканированием |
|----------------------------|------------------------------|
| ![test_photo_2bpp.png](res/test_photo_2bpp.png) | ![test_photo_2bpp_alternating.png](res/test_photo_2bpp_alternating.png) |

**3 bpp (8 уровней яркости):**

| Стандартный Floyd-Steinberg | С чередующимся сканированием |
|----------------------------|------------------------------|
| ![test_photo_3bpp.png](res/test_photo_3bpp.png) | ![test_photo_3bpp_alternating.png](res/test_photo_3bpp_alternating.png) |

---

## Лабораторная работа №3: Построение и заполнение полигонов

### Задание

1. Вычерчивание отрезков (алгоритм Брезенхема)
2. Построение штриховых линий
3. Вывод полигонов на экран
4. Определение типа полигона (простой/сложный, выпуклый/невыпуклый)
5. Заполнение полигонов (правила even-odd и non-zero-winding)

### Результаты

#### Штриховые линии

![Dashed Lines Demo](res/dashed_lines_demo.png)

#### Сравнение правил заполнения

**Треугольник (простой, выпуклый):**

| Контур | Even-Odd Fill | Non-Zero Winding |
|--------|---------------|------------------|
| ![triangle_outline](res/polygon_triangle__simple__convex__outline.png) | ![triangle_evenodd](res/polygon_triangle__simple__convex__evenodd.png) | ![triangle_nonzero](res/polygon_triangle__simple__convex__nonzero.png) |

**Звезда (простой, невыпуклый):**

| Контур | Even-Odd Fill | Non-Zero Winding |
|--------|---------------|------------------|
| ![star_outline](res/polygon_star__simple__non_convex__outline.png) | ![star_evenodd](res/polygon_star__simple__non_convex__evenodd.png) | ![star_nonzero](res/polygon_star__simple__non_convex__nonzero.png) |

**Пентаграмма (сложный, с самопересечениями):**

| Контур | Even-Odd Fill | Non-Zero Winding |
|--------|---------------|------------------|
| ![pentagram_outline](res/polygon_pentagram__complex__self_intersecting__outline.png) | ![pentagram_evenodd](res/polygon_pentagram__complex__self_intersecting__evenodd.png) | ![pentagram_nonzero](res/polygon_pentagram__complex__self_intersecting__nonzero.png) |

**Шестиугольник (простой, выпуклый):**

| Контур | Even-Odd Fill | Non-Zero Winding |
|--------|---------------|------------------|
| ![hexagon_outline](res/polygon_hexagon__simple__convex__outline.png) | ![hexagon_evenodd](res/polygon_hexagon__simple__convex__evenodd.png) | ![hexagon_nonzero](res/polygon_hexagon__simple__convex__nonzero.png) |

---

## Лабораторная работа №4: Кривые Безье, отсечение отрезков

### Задание

1. Построение кривых Безье третьего порядка
2. Отсечение отрезков выпуклым полигоном (алгоритм Кируса-Бека)

### Результаты

#### Кубические кривые Безье

![Bezier Cubic Demo](res/bezier_cubic_demo.png)

| Простая кривая | S-образная кривая | Кривая с петлей |
|----------------|-------------------|-----------------|
| ![Bezier 1](res/bezier_1_simple.png) | ![Bezier 2](res/bezier_2_s_curve.png) | ![Bezier 3](res/bezier_3_loop.png) |

#### Отсечение полигонов алгоритмом Кируса-Бека

| Исходный полигон | Отсекающий | Результат |
|------------------|------------|-----------|
| Звезда (10 вершин) | Квадрат | ![Star clipped](res/polygon_clip_star_by_square.png) |
| Прямоугольник | Шестиугольник | ![Rect clipped](res/polygon_clip_rect_by_hexagon.png) |
| Треугольник | Треугольник | ![Tri clipped](res/polygon_clip_tri_by_tri.png) |
| L-образный | Восьмиугольник | ![L clipped](res/polygon_clip_L_by_octagon.png) |

#### Комплексные тесты отсечения

![Triangle Clipping](res/clip_01_triangle.png)
![Square Diag](res/clip_03_square_diagonals.png)

| Лучевые линии | Параллельные линии |
|---------------|-------------------|
| ![Pentagon Rays](res/clip_04_pentagon_rays.png) | ![Pentagon Parallel](res/clip_05_pentagon_parallel.png) |

![Hexagon](res/clip_06_hexagon.png)
![Boundaries](res/clip_07_boundaries.png)
![Tilted Triangle](res/clip_08_tilted_triangle.png)
![Diamond](res/clip_09_diamond.png)

---

## Лабораторная работа №5: 3D проекции и анимация

### Задание

1. Параллельная проекция повернутого параллелепипеда на плоскость Z=0
2. Одноточечная перспективная проекция параллелепипеда
3. Удаление невидимых ребер (Back-Face Culling)
4. Анимация вращения вокруг произвольной оси

### Результаты

#### Параллельная проекция при разных углах

| 0° | 30° | 45° | 60° |
|----|-----|-----|-----|
| ![0°](res/lab5_parallel_00.png) | ![30°](res/lab5_parallel_30.png) | ![45°](res/lab5_parallel_45.png) | ![60°](res/lab5_parallel_60.png) |

#### Перспективная проекция при разных углах

| 0° | 30° | 45° | 60° |
|----|-----|-----|-----|
| ![0°](res/lab5_perspective_00.png) | ![30°](res/lab5_perspective_30.png) | ![45°](res/lab5_perspective_45.png) | ![60°](res/lab5_perspective_60.png) |

#### Сравнение с удалением невидимых линий и без

| Без удаления | С удалением |
|--------------|-------------|
| ![No removal](res/lab5_no_removal.png) | ![With removal](res/lab5_with_removal.png) |

### Дополнительное задание: Два кубоида на орбите (Lab5Extra.java)

Анимация двух цветных кубоидов, вращающихся на круговой орбите с разными типами двухточечной перспективной проекции.

![Lab5 Extra Animation](res/lab5_extra.gif)

---

## Домашняя работа (HomeWork.java)

### Задание 1: Отсечение полигонов (Вейлер-Айзертон)

**Пятиугольник отсекается квадратом:**

| Исходные полигоны | Результат отсечения |
|-------------------|---------------------|
| ![Setup](res/hw_clip_setup.png) | ![Result](res/hw_clip_result.png) |

**Треугольник отсекается шестиугольником:**

| Исходные полигоны | Результат отсечения |
|-------------------|---------------------|
| ![Setup](res/hw_clip_tri_hex_setup.png) | ![Result](res/hw_clip_tri_hex_result.png) |

---

### Задание 2: Кривая Эрмита

| Кривая Эрмита | С подписями |
|---------------|-------------|
| ![Hermite](res/hw_hermite_curve.png) | ![Labeled](res/hw_hermite_curve_labeled.png) |

---

### Задание 3: Гистограммы CMYK с GCR

Тестовое изображение:

![Color Test](res/hw_color_test.png)

**GCR = 0% (без замещения):**

| Cyan | Magenta | Yellow | Black |
|------|---------|--------|-------|
| ![C](res/hw_hist_c.png) | ![M](res/hw_hist_m.png) | ![Y](res/hw_hist_y.png) | ![K](res/hw_hist_k.png) |

**GCR = 50% (средняя замена):**

| Cyan | Magenta | Yellow | Black |
|------|---------|--------|-------|
| ![C](res/hw_hist_c_gcr50.png) | ![M](res/hw_hist_m_gcr50.png) | ![Y](res/hw_hist_y_gcr50.png) | ![K](res/hw_hist_k_gcr50.png) |

**GCR = 100% (максимальная замена):**

| Cyan | Magenta | Yellow | Black |
|------|---------|--------|-------|
| ![C](res/hw_hist_c_gcr100.png) | ![M](res/hw_hist_m_gcr100.png) | ![Y](res/hw_hist_y_gcr100.png) | ![K](res/hw_hist_k_gcr100.png) |

---
